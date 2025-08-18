package com.disrupton.Geolocalizacion.service;

import com.disrupton.Geolocalizacion.dto.LocationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.time.LocalDateTime;

@Service
@Slf4j
public class GeolocalizacionService {

    private final RestTemplate restTemplate;
    private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org";
    private static final String USER_AGENT = "DisruptonApp/1.0 (disrupton@utec.edu.pe)";

    // Rate limiting: máximo 1 petición por segundo (requerido por Nominatim)
    private static final long RATE_LIMIT_DELAY_MS = 1000; // 1 segundo
    private volatile LocalDateTime lastRequestTime = LocalDateTime.now();

    public GeolocalizacionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Aplica rate limiting para cumplir con los términos de Nominatim
     */
    private void applyRateLimit() {
        LocalDateTime now = LocalDateTime.now();
        long timeSinceLastRequest = java.time.Duration.between(lastRequestTime, now).toMillis();

        if (timeSinceLastRequest < RATE_LIMIT_DELAY_MS) {
            long sleepTime = RATE_LIMIT_DELAY_MS - timeSinceLastRequest;
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Rate limiting interrumpido", e);
            }
        }

        lastRequestTime = LocalDateTime.now();
    }

    /**
     * Obtiene información de ubicación a partir de coordenadas (reverse geocoding)
     */
    public LocationDto reverseGeocode(Double lat, Double lon) {
        // Validar parámetros
        if (lat == null || lon == null) {
            throw new IllegalArgumentException("Latitud y longitud son requeridas");
        }

        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Latitud debe estar entre -90 y 90");
        }

        if (lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Longitud debe estar entre -180 y 180");
        }

        log.info("Obteniendo información de ubicación para coordenadas: lat={}, lon={}", lat, lon);

        // Aplicar rate limiting
        applyRateLimit();

        String url = UriComponentsBuilder
                .fromHttpUrl(NOMINATIM_BASE_URL)
                .path("/reverse")
                .queryParam("format", "json")
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("addressdetails", "1")
                .queryParam("accept-language", "es")
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT); // Requerido por Nominatim
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Error al consultar Nominatim. Status: {}", response.getStatusCode());
                throw new RuntimeException("Error al consultar el servicio de geolocalización");
            }

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new RuntimeException("Respuesta vacía del servicio de geolocalización");
            }

            // Verificar si hay error en la respuesta de Nominatim
            if (body.containsKey("error")) {
                String errorMessage = (String) body.get("error");
                log.error("Error de Nominatim: {}", errorMessage);
                throw new RuntimeException("Error del servicio de geolocalización: " + errorMessage);
            }

            Map<String, Object> address = (Map<String, Object>) body.get("address");
            String displayName = (String) body.get("display_name");

            if (address == null) {
                log.warn("No se encontró información de dirección para las coordenadas: lat={}, lon={}", lat, lon);
                // Retornar objeto con solo coordenadas
                LocationDto location = new LocationDto();
                location.setLatitude(lat);
                location.setLongitude(lon);
                location.setFullAddress(displayName != null ? displayName : "Ubicación desconocida");
                return location;
            }

            LocationDto location = new LocationDto();
            location.setDepartment((String) address.getOrDefault("state", null));
            location.setDistrict((String) address.getOrDefault("suburb", null));
            location.setStreet((String) address.getOrDefault("road", null));
            location.setCity((String) address.getOrDefault("city", null));
            location.setCountry((String) address.getOrDefault("country", null));
            location.setPostalCode((String) address.getOrDefault("postcode", null));
            location.setLatitude(lat);
            location.setLongitude(lon);
            location.setFullAddress(displayName);

            log.info("Información de ubicación obtenida: {}", location);
            return location;

        } catch (Exception e) {
            log.error("Error al obtener información de ubicación: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener información de ubicación: " + e.getMessage());
        }
    }

    /**
     * Obtiene coordenadas a partir de una dirección (forward geocoding)
     */
    public LocationDto forwardGeocode(String address) {
        // Validar parámetros
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("La dirección es requerida");
        }

        String cleanAddress = address.trim();
        if (cleanAddress.length() < 3) {
            throw new IllegalArgumentException("La dirección debe tener al menos 3 caracteres");
        }

        log.info("Obteniendo coordenadas para dirección: {}", cleanAddress);

        // Aplicar rate limiting
        applyRateLimit();

        String url = UriComponentsBuilder
                .fromHttpUrl(NOMINATIM_BASE_URL)
                .path("/search")
                .queryParam("format", "json")
                .queryParam("q", cleanAddress)
                .queryParam("limit", "1")
                .queryParam("addressdetails", "1")
                .queryParam("accept-language", "es")
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map[]> response = restTemplate.exchange(url, HttpMethod.GET, request, Map[].class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Error al consultar Nominatim. Status: {}", response.getStatusCode());
                throw new RuntimeException("Error al consultar el servicio de geolocalización");
            }

            Map[] results = response.getBody();
            if (results == null || results.length == 0) {
                throw new RuntimeException("No se encontró la dirección especificada: " + cleanAddress);
            }

            Map<String, Object> result = results[0];
            Map<String, Object> addressDetails = (Map<String, Object>) result.get("address");

            // Validar que las coordenadas sean válidas
            String latStr = (String) result.get("lat");
            String lonStr = (String) result.get("lon");

            if (latStr == null || lonStr == null) {
                throw new RuntimeException("Coordenadas inválidas en la respuesta");
            }

            Double lat = Double.parseDouble(latStr);
            Double lon = Double.parseDouble(lonStr);
            String displayName = (String) result.get("display_name");

            LocationDto location = new LocationDto();

            if (addressDetails != null) {
                location.setDepartment((String) addressDetails.getOrDefault("state", null));
                location.setDistrict((String) addressDetails.getOrDefault("suburb", null));
                location.setStreet((String) addressDetails.getOrDefault("road", null));
                location.setCity((String) addressDetails.getOrDefault("city", null));
                location.setCountry((String) addressDetails.getOrDefault("country", null));
                location.setPostalCode((String) addressDetails.getOrDefault("postcode", null));
            }

            location.setLatitude(lat);
            location.setLongitude(lon);
            location.setFullAddress(displayName);

            log.info("Coordenadas obtenidas: lat={}, lon={}", lat, lon);
            return location;

        } catch (Exception e) {
            log.error("Error al obtener coordenadas: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener coordenadas: " + e.getMessage());
        }
    }
}