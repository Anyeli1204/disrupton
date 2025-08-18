package com.disrupton.Geolocalizacion.service;

import com.disrupton.Geolocalizacion.dto.LocationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Service
@Slf4j
public class IPGeolocationService {

    private final RestTemplate restTemplate;
    private static final String IP_API_BASE_URL = "http://ip-api.com/json";

    public IPGeolocationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Obtiene la ubicación basada en la IP del usuario
     */
    public LocationDto getLocationFromIP(HttpServletRequest request) {
        try {
            String clientIP = getClientIPAddress(request);
            log.info("Obteniendo ubicación para IP: {}", clientIP);

            // Para IPs locales o de desarrollo, usar ubicación por defecto
            if (isLocalIP(clientIP)) {
                log.info("IP local detectada, usando ubicación por defecto");
                return getDefaultLocation();
            }

            String url = IP_API_BASE_URL + "/" + clientIP;
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> httpRequest = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, httpRequest, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = response.getBody();

                // Verificar si la consulta fue exitosa
                if ("success".equals(data.get("status"))) {
                    LocationDto location = new LocationDto();
                    location.setLatitude(Double.parseDouble(data.get("lat").toString()));
                    location.setLongitude(Double.parseDouble(data.get("lon").toString()));
                    location.setCity((String) data.get("city"));
                    location.setCountry((String) data.get("country"));
                    location.setDepartment((String) data.get("regionName"));
                    location.setPostalCode((String) data.get("zip"));
                    location.setFullAddress(String.format("%s, %s, %s",
                            data.get("city"), data.get("regionName"), data.get("country")));

                    log.info("Ubicación obtenida por IP: {}", location.getFullAddress());
                    return location;
                } else {
                    log.warn("No se pudo obtener ubicación para IP: {}", clientIP);
                    return getDefaultLocation();
                }
            } else {
                log.warn("Error al consultar servicio de IP geolocalización");
                return getDefaultLocation();
            }

        } catch (Exception e) {
            log.error("Error al obtener ubicación por IP: {}", e.getMessage(), e);
            return getDefaultLocation();
        }
    }

    /**
     * Obtiene la dirección IP real del cliente
     */
    private String getClientIPAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty() && !"unknown".equalsIgnoreCase(xRealIP)) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }

    /**
     * Verifica si la IP es local
     */
    private boolean isLocalIP(String ip) {
        return ip == null ||
                ip.equals("127.0.0.1") ||
                ip.equals("0:0:0:0:0:0:0:1") ||
                ip.startsWith("192.168.") ||
                ip.startsWith("10.") ||
                ip.startsWith("172.");
    }

    /**
     * Retorna ubicación por defecto (Lima, Perú)
     */
    private LocationDto getDefaultLocation() {
        LocationDto defaultLocation = new LocationDto();
        defaultLocation.setLatitude(-12.0464);
        defaultLocation.setLongitude(-77.0428);
        defaultLocation.setCity("Lima");
        defaultLocation.setCountry("Perú");
        defaultLocation.setDepartment("Lima");
        defaultLocation.setFullAddress("Lima, Perú (ubicación por defecto)");
        return defaultLocation;
    }
}