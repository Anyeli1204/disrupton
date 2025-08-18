package com.disrupton.cultural.service;

import com.disrupton.Geolocalizacion.dto.LocationDto;
import com.disrupton.Geolocalizacion.service.GeolocalizacionService;
import com.disrupton.Geolocalizacion.service.IPGeolocationService;
import com.disrupton.KiriEngine.model.ImageUploadRequest;
import com.disrupton.KiriEngine.model.KiriEngineResponse;
import com.disrupton.comment.model.Comment;
import com.disrupton.cultural.model.CulturalObject;
import com.disrupton.cultural.model.CulturalUploadRequest;
import com.disrupton.cultural.dto.CulturalObjectDto;
import com.disrupton.KiriEngine.service.KiriEngineService;
import com.disrupton.reaction.model.Reaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class CulturalService {
    
    private final KiriEngineService kiriEngineService;
    private final GeolocalizacionService geolocalizacionService;
    private final IPGeolocationService ipGeolocationService;
    private final FirebaseCulturalObjectService firebaseCulturalObjectService;
    
    /**
     * Guardar objeto cultural en Firebase
     */
    public CulturalObject saveCulturalObject(CulturalObject culturalObject) {
        try {
            log.info("🏺 Guardando objeto cultural: {}", culturalObject.getName());
            
            // Convertir CulturalObject a CulturalObjectDto
            CulturalObjectDto dto = new CulturalObjectDto();
            dto.setTitle(culturalObject.getName());
            dto.setDescription(culturalObject.getDescription());
            dto.setCreatedBy(culturalObject.getContributorId());
            dto.setStatus(culturalObject.getStatus());
            dto.setModelUrl(culturalObject.getModelUrl());
            
            CulturalObjectDto savedDto = firebaseCulturalObjectService.saveCulturalObject(dto);
            
            // Asignar el ID generado al objeto cultural
            culturalObject.setId(savedDto.getId());
            return culturalObject;
            
        } catch (Exception e) {
            log.error("❌ Error al guardar objeto cultural: {}", e.getMessage(), e);
            throw new RuntimeException("Error al guardar objeto cultural", e);
        }
    }
    
    /**
     * Subir objeto cultural completo con imágenes y contexto
     */
    public CulturalObject uploadCulturalObject(CulturalUploadRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) throws IOException {
        log.info("Procesando objeto cultural: {}", request.getName());
        
        // Validar información cultural
        request.validateCulturalInfo();
        
        // Validar imágenes
        request.validateImages();
        
        // Crear objeto cultural en estado DRAFT
        CulturalObject culturalObject = new CulturalObject();
        culturalObject.setName(request.getName());
        culturalObject.setDescription(request.getDescription());
        culturalObject.setOrigin(request.getOrigin());
        culturalObject.setCulturalType(request.getCulturalType());
        culturalObject.setLocalPhrases(request.getLocalPhrases());
        culturalObject.setStory(request.getStory());
        culturalObject.setRegion(request.getRegion());
        culturalObject.setCaptureNotes(request.getCaptureNotes());
        culturalObject.setFileFormat(request.getFileFormat());
        culturalObject.setNumberOfImages(request.getImagesFiles().size());
        culturalObject.setStatus(CulturalObject.Status.PENDING_REVIEW.name());
        culturalObject.setCreatedAt(LocalDateTime.now());
        culturalObject.setUpdatedAt(LocalDateTime.now());
        
        // Procesar información de ubicación
        processLocationInfo(culturalObject, request, httpRequest);
        
        // TODO: Obtener usuario real desde base de datos
        culturalObject.setContributorId(request.getUserId().toString());
        
        // Procesar imágenes con KIRI Engine
        ImageUploadRequest kiriRequest = new ImageUploadRequest();
        kiriRequest.setImagesFiles(request.getImagesFiles());
        kiriRequest.setFileFormat(request.getFileFormat());
        kiriRequest.setModelQuality(1); // Medium quality para objetos culturales
        kiriRequest.setTextureQuality(1); // 2K texture
        kiriRequest.setIsMask(1); // Auto masking
        kiriRequest.setTextureSmoothing(1); // Texture smoothing
        
        log.info("Enviando {} imágenes a KIRI Engine para objeto cultural: {}", 
                request.getImagesFiles().size(), request.getName());
        
        // Llamar a KIRI Engine
        KiriEngineResponse kiriResponse = kiriEngineService.uploadImages(kiriRequest);
        
        if (kiriResponse != null && kiriResponse.getOk()) {
            culturalObject.setKiriEngineSerial(kiriResponse.getData().getSerialize());
            log.info("Objeto cultural procesado exitosamente. Serial: {}", 
                    kiriResponse.getData().getSerialize());
        } else {
            throw new RuntimeException("Error al procesar imágenes en KIRI Engine");
        }
        
        // TODO: Guardar en base de datos
        // culturalObjectRepository.save(culturalObject);
        
        return culturalObject;
    }
    
    /**
     * Obtener objetos culturales con filtros
     */
    public List<CulturalObject> getCulturalObjects(String region, String culturalType, int page, int size) {
        // TODO: Implementar consulta a base de datos con filtros
        log.info("Consultando objetos culturales - Región: {}, Tipo: {}, Página: {}, Tamaño: {}", 
                region, culturalType, page, size);
        
        // Mock data por ahora
        return List.of();
    }
    
    /**
     * Obtener objeto cultural por ID
     */
    public CulturalObject getCulturalObjectById(String id) {
        // TODO: Implementar consulta a base de datos
        log.info("Consultando objeto cultural con ID: {}", id);
        
        // Mock data por ahora
        return new CulturalObject();
    }
    
    /**
     * Agregar comentario
     */
    public Comment addComment(String objectId, String content, String userId, String parentCommentId) {
        log.info("Agregando comentario al objeto cultural {} por usuario {}", objectId, userId);
        
        Comment comment = new Comment();
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setIsModerated(false);
        comment.setParentCommentId(parentCommentId);
        
        // TODO: Obtener usuario y objeto cultural reales
        comment.setAuthorId(userId);
        comment.setCulturalObjectId(objectId);
        
        // TODO: Guardar en base de datos
        // commentRepository.save(comment);
        
        return comment;
    }
    
    /**
     * Agregar reacción
     */
    public Reaction addReaction(String objectId, String type, String userId) {
        log.info("Agregando reacción {} al objeto cultural {} por usuario {}", type, objectId, userId);
        
        Reaction reaction = new Reaction();
        reaction.setType(type);
        reaction.setCreatedAt(LocalDateTime.now());
        
        // TODO: Obtener usuario y objeto cultural reales
        reaction.setUserId(userId);
        reaction.setCulturalObjectId(objectId);
        
        // TODO: Guardar en base de datos
        // reactionRepository.save(reaction);
        
        return reaction;
    }
    
    /**
     * Obtener objetos pendientes de moderación
     */
    public List<CulturalObject> getPendingObjects(String moderatorId) {
        // TODO: Verificar que el usuario sea moderador
        log.info("Consultando objetos pendientes de moderación para moderador: {}", moderatorId);
        
        // TODO: Implementar consulta a base de datos
        return List.of();
    }
    
    /**
     * Revisar objeto cultural (aprobar/rechazar)
     */
    public CulturalObject reviewObject(String objectId, String moderatorId, String status, String feedback) {
        log.info("Revisando objeto cultural {} por moderador {} con estado: {}", 
                objectId, moderatorId, status);
        
        // TODO: Verificar que el usuario sea moderador
        // TODO: Obtener objeto cultural real
        CulturalObject culturalObject = new CulturalObject();
        culturalObject.setId(objectId);
        culturalObject.setStatus(status);
        culturalObject.setUpdatedAt(LocalDateTime.now());
        
        // TODO: Obtener moderador real
        culturalObject.setModeratorId(moderatorId);
        
        // TODO: Guardar en base de datos
        // culturalObjectRepository.save(culturalObject);
        
        return culturalObject;
    }
    
    /**
     * Obtener estadísticas culturales
     */
    public Map<String, Object> getStatistics() {
        log.info("Generando estadísticas culturales");
        
        Map<String, Object> stats = new HashMap<>();
        
        // TODO: Implementar consultas reales a base de datos
        stats.put("totalObjects", 0);
        stats.put("approvedObjects", 0);
        stats.put("pendingObjects", 0);
        stats.put("totalComments", 0);
        stats.put("totalReactions", 0);
        stats.put("objectsByRegion", new HashMap<>());
        stats.put("objectsByType", new HashMap<>());
        
        return stats;
    }
    
    /**
     * Procesa la información de ubicación del objeto cultural
     * GARANTIZA que siempre se asigne una ubicación al objeto
     */

    private void processLocationInfo(CulturalObject culturalObject, CulturalUploadRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        try {
            LocationDto locationInfo;
            
            // Prioridad 1: Si se solicita geolocalización automática explícitamente
            if (Boolean.TRUE.equals(request.getAutoLocation())) {
                log.info("Solicitada geolocalización automática para el objeto cultural");
                locationInfo = ipGeolocationService.getLocationFromIP(httpRequest);
            }
            // Prioridad 2: Si se proporcionan coordenadas, usar reverse geocoding
            else if (request.getLatitude() != null && request.getLongitude() != null) {
                log.info("Obteniendo información de ubicación desde coordenadas: lat={}, lon={}", 
                        request.getLatitude(), request.getLongitude());
                locationInfo = geolocalizacionService.reverseGeocode(request.getLatitude(), request.getLongitude());
            }
            // Prioridad 3: Si se proporciona una dirección, usar forward geocoding
            else if (request.getAddress() != null && !request.getAddress().trim().isEmpty()) {
                log.info("Obteniendo coordenadas desde dirección: {}", request.getAddress());
                locationInfo = geolocalizacionService.forwardGeocode(request.getAddress());
            }
            // Prioridad 4: Geolocalización automática por defecto (SIEMPRE se ejecuta)
            else {
                log.info("No se proporcionó ubicación específica, usando geolocalización automática por defecto");
                locationInfo = ipGeolocationService.getLocationFromIP(httpRequest);
            }
            
            // GARANTIZAR que siempre se asigne ubicación
            if (locationInfo != null) {
                culturalObject.setLatitude(locationInfo.getLatitude());
                culturalObject.setLongitude(locationInfo.getLongitude());
                culturalObject.setDepartment(locationInfo.getDepartment());
                culturalObject.setDistrict(locationInfo.getDistrict());
                culturalObject.setStreet(locationInfo.getStreet());
                culturalObject.setCity(locationInfo.getCity());
                culturalObject.setCountry(locationInfo.getCountry());
                culturalObject.setPostalCode(locationInfo.getPostalCode());
                culturalObject.setFullAddress(locationInfo.getFullAddress());
                
                log.info("Información de ubicación asignada al objeto cultural: {}", locationInfo.getFullAddress());
            } else {
                // Fallback final: ubicación por defecto
                log.warn("No se pudo obtener información de ubicación, usando ubicación por defecto");
                LocationDto defaultLocation = getDefaultLocation();
                culturalObject.setLatitude(defaultLocation.getLatitude());
                culturalObject.setLongitude(defaultLocation.getLongitude());
                culturalObject.setDepartment(defaultLocation.getDepartment());
                culturalObject.setDistrict(defaultLocation.getDistrict());
                culturalObject.setStreet(defaultLocation.getStreet());
                culturalObject.setCity(defaultLocation.getCity());
                culturalObject.setCountry(defaultLocation.getCountry());
                culturalObject.setPostalCode(defaultLocation.getPostalCode());
                culturalObject.setFullAddress(defaultLocation.getFullAddress());
                
                log.info("Ubicación por defecto asignada al objeto cultural: {}", defaultLocation.getFullAddress());
            }
            
        } catch (Exception e) {
            log.error("Error al procesar información de ubicación: {}", e.getMessage(), e);
            // Fallback final en caso de error: ubicación por defecto
            log.info("Aplicando fallback de ubicación por defecto debido a error");
            LocationDto defaultLocation = getDefaultLocation();
            culturalObject.setLatitude(defaultLocation.getLatitude());
            culturalObject.setLongitude(defaultLocation.getLongitude());
            culturalObject.setDepartment(defaultLocation.getDepartment());
            culturalObject.setDistrict(defaultLocation.getDistrict());
            culturalObject.setStreet(defaultLocation.getStreet());
            culturalObject.setCity(defaultLocation.getCity());
            culturalObject.setCountry(defaultLocation.getCountry());
            culturalObject.setPostalCode(defaultLocation.getPostalCode());
            culturalObject.setFullAddress(defaultLocation.getFullAddress());
            
            log.info("Ubicación por defecto asignada al objeto cultural (fallback): {}", defaultLocation.getFullAddress());
        }
    }
    
    /**
     * Retorna ubicación por defecto (Lima, Perú)
     * Se usa como fallback cuando no se puede obtener ubicación
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