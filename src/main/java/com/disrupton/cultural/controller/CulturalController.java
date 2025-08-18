package com.disrupton.cultural.controller;

import com.disrupton.comment.model.Comment;
import com.disrupton.cultural.model.CulturalObject;
import com.disrupton.cultural.service.CulturalService;
import com.disrupton.cultural.model.CulturalUploadRequest;
import com.disrupton.cultural.dto.CulturalObjectRequest;
import com.disrupton.reaction.model.Reaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/cultural")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CulturalController {
    
    private final CulturalService culturalService;
    
    /**
     * Crear objeto cultural sin archivos (solo datos)
     */
    @PostMapping("/objects")
    public ResponseEntity<?> createCulturalObject(@RequestBody CulturalObjectRequest request) {
        try {
            log.info("🏺 Creando objeto cultural: {}", request.getName());
            
            // Crear objeto cultural
            CulturalObject culturalObject = new CulturalObject();
            culturalObject.setName(request.getName());
            culturalObject.setDescription(request.getDescription());
            culturalObject.setOrigin(request.getOrigin());
            culturalObject.setCulturalType(request.getCulturalType());
            culturalObject.setLocalPhrases(request.getLocalPhrases());
            culturalObject.setStory(request.getStory());
            culturalObject.setContributorId(request.getContributorId());
            culturalObject.setNumberOfImages(request.getNumberOfImages());
            culturalObject.setCaptureNotes(request.getCaptureNotes());
            culturalObject.setRegion(request.getRegion());
            culturalObject.setStatus("DRAFT");
            culturalObject.setCreatedAt(java.time.LocalDateTime.now());
            culturalObject.setUpdatedAt(java.time.LocalDateTime.now());
            
            // Guardar en Firebase
            CulturalObject savedObject = culturalService.saveCulturalObject(culturalObject);
            
            return ResponseEntity.ok(savedObject);
            
        } catch (Exception e) {
            log.error("❌ Error al crear objeto cultural: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    new ErrorResponse("Error interno del servidor", e.getMessage()));
        }
    }
    
    /**
     * Subir objeto cultural con imágenes y contexto
     */
    @PostMapping("/upload-object")
    public ResponseEntity<?> uploadCulturalObject(
            @RequestParam("imagesFiles") List<MultipartFile> imagesFiles,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("origin") String origin,
            @RequestParam("culturalType") String culturalType,
            @RequestParam("region") String region,
            @RequestParam(value = "localPhrases", required = false) String localPhrases,
            @RequestParam(value = "story", required = false) String story,
            @RequestParam(value = "captureNotes", required = false) String captureNotes,
            @RequestParam(value = "fileFormat", defaultValue = "GLB") String fileFormat,
            @RequestParam("userId") String userId,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "autoLocation", required = false, defaultValue = "false") Boolean autoLocation,
            HttpServletRequest httpRequest) {
        try {
            log.info("Recibida solicitud para subir objeto cultural: {}", name);
            
            // Crear objeto de solicitud
            CulturalUploadRequest request = new CulturalUploadRequest();
            request.setImagesFiles(imagesFiles);
            request.setName(name);
            request.setDescription(description);
            request.setOrigin(origin);
            request.setCulturalType(culturalType);
            request.setRegion(region);
            request.setLocalPhrases(localPhrases);
            request.setStory(story);
            request.setCaptureNotes(captureNotes);
            request.setFileFormat(fileFormat);
            request.setUserId(userId);
            request.setLatitude(latitude);
            request.setLongitude(longitude);
            request.setAddress(address);
            request.setAutoLocation(autoLocation);
            
            // Procesar solicitud
            CulturalObject result = culturalService.uploadCulturalObject(request, httpRequest);
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    new ErrorResponse("Error de validación", e.getMessage()));
        } catch (Exception e) {
            log.error("Error inesperado: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                    new ErrorResponse("Error interno del servidor", e.getMessage()));
        }
    }
    
    /**
     * Obtener todos los objetos culturales aprobados
     */
    @GetMapping("/objects")
    public ResponseEntity<List<CulturalObject>> getCulturalObjects(
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "culturalType", required = false) String culturalType,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        
        List<CulturalObject> objects = culturalService.getCulturalObjects(region, culturalType, page, size);
        return ResponseEntity.ok(objects);
    }
    
    /**
     * Obtener objeto cultural por ID
     */
    @GetMapping("/objects/{id}")
    public ResponseEntity<CulturalObject> getCulturalObject(@PathVariable String id) {
        CulturalObject object = culturalService.getCulturalObjectById(id);
        return ResponseEntity.ok(object);
    }
    
    /**
     * Agregar comentario a un objeto cultural
     */
    @PostMapping("/objects/{id}/comments")
    public ResponseEntity<Comment> addComment(
            @PathVariable String id,
            @RequestParam("content") String content,
            @RequestParam("userId") String userId,
            @RequestParam(value = "parentCommentId", required = false) String parentCommentId) {
        
        Comment comment = culturalService.addComment(id, content, userId, parentCommentId);
        return ResponseEntity.ok(comment);
    }
    
    /**
     * Agregar reacción a un objeto cultural
     */
    @PostMapping("/objects/{id}/reactions")
    public ResponseEntity<Reaction> addReaction(
            @PathVariable String id,
            @RequestParam("type") String type,
            @RequestParam("userId") String userId) {
        
        Reaction reaction = culturalService.addReaction(id, type, userId);
        return ResponseEntity.ok(reaction);
    }
    
    /**
     * Obtener objetos pendientes de moderación (solo moderadores)
     */
    @GetMapping("/moderation/pending")
    public ResponseEntity<List<CulturalObject>> getPendingObjects(
            @RequestParam("moderatorId") String moderatorId) {
        
        List<CulturalObject> objects = culturalService.getPendingObjects(moderatorId);
        return ResponseEntity.ok(objects);
    }
    
    /**
     * Aprobar o rechazar objeto cultural (solo moderadores)
     */
    @PostMapping("/moderation/{id}/review")
    public ResponseEntity<CulturalObject> reviewObject(
            @PathVariable String id,
            @RequestParam("moderatorId") String moderatorId,
            @RequestParam("status") String status,
            @RequestParam(value = "feedback", required = false) String feedback) {
        
        CulturalObject object = culturalService.reviewObject(id, moderatorId, status, feedback);
        return ResponseEntity.ok(object);
    }
    
    /**
     * Obtener estadísticas culturales
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        return ResponseEntity.ok(culturalService.getStatistics());
    }
    
    /**
     * Clase para respuestas de error
     */
    public static class ErrorResponse {
        private String error;
        private String message;
        
        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
} 