package com.disrupton.payment.controller;

import com.disrupton.user.service.FirebaseUserService;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/culqui")
@RequiredArgsConstructor
@Slf4j
public class CulquiWebhookController {

    private final FirebaseUserService userService;
    private final Gson gson = new Gson();

    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String payload, @RequestHeader Map<String,String> headers) {
        try {
            log.info("Webhook Culqui recibido: {}", payload);
            Map<?,?> event = gson.fromJson(payload, Map.class);
            Map<?,?> data = (Map<?,?>) event.get("data");
            Map<?,?> object = data != null ? (Map<?,?>) data.get("object") : null;
            String type = (String) event.get("type");

            // Ejemplo: si es una suscripción creada/paid -> actualizar suscripcion en usuario
            if (type != null && type.startsWith("subscription")) {
                // extraer customer_id o metadata para identificar usuario y plan
                // Depende de cómo envíes metadata al crear la suscripción
                Map<?,?> metadata = object != null ? (Map<?,?>) object.get("metadata") : null;
                if (metadata != null && metadata.get("userId") != null) {
                    String userId = metadata.get("userId").toString();
                    // marca suscripcion activa 30 dias desde ahora o según info de objeto
                    var user = userService.getUserById(userId);
                    java.time.Instant now = java.time.Instant.now();
                    java.time.Instant exp = now.plus(java.time.Duration.ofDays(30));
                    user.setSuscripcionExpira(java.util.Date.from(exp));
                    userService.updateUser(userId, user);
                }
            }

            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            log.error("Error procesando webhook Culqi", e);
            return ResponseEntity.status(500).body("error");
        }
    }
}
