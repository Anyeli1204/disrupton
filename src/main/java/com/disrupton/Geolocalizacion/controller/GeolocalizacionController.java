package com.disrupton.Geolocalizacion.controller;

import com.disrupton.Geolocalizacion.dto.LocationDto;
import com.disrupton.Geolocalizacion.service.GeolocalizacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/geolocalizacion")
@RequiredArgsConstructor
@Slf4j
public class GeolocalizacionController {

    private final GeolocalizacionService geolocalizacionService;

    /**
     * Obtiene información de ubicación a partir de coordenadas
     */
    @GetMapping("/reverse")
    public ResponseEntity<?> reverseGeocode(
            @RequestParam Double lat,
            @RequestParam Double lon) {
        
        log.info("Solicitud de reverse geocoding: lat={}, lon={}", lat, lon);
        
        try {
            LocationDto location = geolocalizacionService.reverseGeocode(lat, lon);
            return ResponseEntity.ok(location);
        } catch (IllegalArgumentException e) {
            log.warn("Parámetros inválidos en reverse geocoding: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Parámetros inválidos");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error en reverse geocoding: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error interno del servidor");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Obtiene coordenadas a partir de una dirección
     */
    @GetMapping("/forward")
    public ResponseEntity<?> forwardGeocode(
            @RequestParam String address) {
        
        log.info("Solicitud de forward geocoding: address={}", address);
        
        try {
            LocationDto location = geolocalizacionService.forwardGeocode(address);
            return ResponseEntity.ok(location);
        } catch (IllegalArgumentException e) {
            log.warn("Parámetros inválidos en forward geocoding: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Parámetros inválidos");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error en forward geocoding: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error interno del servidor");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
} 