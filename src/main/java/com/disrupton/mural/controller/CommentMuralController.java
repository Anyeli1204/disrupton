package com.disrupton.mural.controller;

import com.disrupton.comment.dto.CommentDto;
import com.disrupton.moderation.ModerationService;
import com.disrupton.mural.model.Mural;
import com.disrupton.mural.service.MuralService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mural")
@Slf4j
@RequiredArgsConstructor
public class CommentMuralController {

    private final ModerationService moderationService;
    private final MuralService muralService;


    @PostMapping("/")
    public ResponseEntity<Map<String, Object>> crearPregunta(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String pregunta = (String) request.get("pregunta");
            List<String> imagenes = (List<String>) request.get("imagenes");

            if (pregunta == null || pregunta.isBlank()) {
                response.put("error", "La pregunta no puede estar vacía.");
                return ResponseEntity.badRequest().body(response);
            }

            Mural nuevaPregunta = muralService.crearPregunta(pregunta, imagenes);
            response.put("mensaje", "✅ Pregunta del mural creada.");
            response.put("id", nuevaPregunta.getId());
            response.put("pregunta", nuevaPregunta.getPregunta());
            response.put("imagenes", nuevaPregunta.getImagenes());
        } catch (Exception e) {
            response.put("error", "❌ No se pudo crear la pregunta del mural.");
            response.put("detalle", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/comentarios/{preguntaId}")
    public ResponseEntity<List<CommentDto>> listarComentariosPorPregunta(@PathVariable String preguntaId) throws Exception {
        List<CommentDto> comentarios = muralService.getCommentsByPreguntaId(preguntaId);
        return ResponseEntity.ok(comentarios);
    }


    @PostMapping("/comentarios")
    public ResponseEntity<?> comentarMural(@RequestBody CommentDto request) {
        String comentario = request.getText();
        String preguntaId = request.getPreguntaId();
        if (comentario == null || comentario.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("El contenido del comentario no puede estar vacío.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("comentario", comentario);
        response.put("preguntaId", preguntaId);

        try {
            long inicio = System.currentTimeMillis();
            boolean esSeguro = moderationService.isCommentSafe(comentario);
            long tiempoRespuesta = System.currentTimeMillis() - inicio;

            response.put("tiempoRespuestaGemini", tiempoRespuesta + " ms");
            response.put("aprobado", esSeguro);

            if (esSeguro) {
                muralService.saveCommentToMural(request);
                response.put("mensaje", "✅ Comentario para mural aprobado y guardado.");
            } else {
                response.put("rechazado", true);
                response.put("motivo", moderationService.getReasonIfUnsafe(comentario));
                response.put("mensaje", "⚠️ Comentario rechazado por contenido inapropiado.");
            }

        } catch (Exception e) {
            response.put("error", e.getMessage());
            response.put("mensaje", "❌ Error interno al procesar comentario del mural.");
        }

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{preguntaId}")
    public ResponseEntity<Map<String, Object>> eliminarComentario(
            @PathVariable String preguntaId,
            @RequestParam String commentId,
            @RequestParam String userId) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("🗑️ Intentando eliminar comentario {} del objeto {} por usuario {}",
                    commentId, preguntaId, userId);

            boolean eliminado = muralService.deleteCommentMural(commentId, userId);

            if (eliminado) {
                response.put("mensaje", "✅ Comentario eliminado exitosamente.");
                response.put("commentId", commentId);
                response.put("preguntaId", preguntaId);
                response.put("eliminado", true);
                log.info("✅ Comentario {} eliminado exitosamente", commentId);
                return ResponseEntity.ok(response);
            } else {
                response.put("mensaje", "❌ No se pudo eliminar el comentario. Verifica que seas el autor.");
                response.put("eliminado", false);
                log.warn("⚠️ No se pudo eliminar comentario {} - Sin permisos o no existe", commentId);
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("❌ Error al eliminar comentario {}: {}", commentId, e.getMessage(), e);
            response.put("error", e.getMessage());
            response.put("mensaje", "❌ Error interno al eliminar comentario.");
            response.put("eliminado", false);
            return ResponseEntity.internalServerError().body(response);
        }
    }

}
