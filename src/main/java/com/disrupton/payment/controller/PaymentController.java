package com.disrupton.payment.controller;

import com.disrupton.payment.dto.CulquiPaymentRequest;
import com.disrupton.payment.model.payment;
import com.disrupton.payment.service.PaymentculquiService;
import com.disrupton.user.service.FirebaseUserService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentculquiService paymentService;
    private final FirebaseUserService userService;

    @PostMapping("/cobrar")
    public ResponseEntity<?> cobrar(@RequestBody CulquiPaymentRequest req) {
        try {
            // Validaciones básicas
            if (req.getUserId() == null || req.getCardToken() == null || req.getPaymentType() == null) {
                return ResponseEntity.badRequest().body("Faltan parámetros requeridos");
            }

            // Verificar usuario existente
            if (userService.getUserById(req.getUserId()) == null) {
                return ResponseEntity.badRequest().body("Usuario no encontrado");
            }

            // Realizar cobro en Culqui y registrar payment
            payment t = paymentService.cobrar(req);

            // Si es suscripción, actualizar usuario con fecha de expiración (30 días)
            if ("SUSCRIPCION".equalsIgnoreCase(t.getTipo())) {
                var user = userService.getUserById(req.getUserId());
                java.time.Instant now = java.time.Instant.now();
                java.time.Instant exp = now.plus(java.time.Duration.ofDays(30));
                user.setSuscripcionExpira(java.util.Date.from(exp));
                // si saveUser crea ID automático, usa update: tu service tiene updateUser(userId,user)
                userService.updateUser(req.getUserId(), user);
            }

            // Si saveCard solicitado, guardar card token para one-click
            if (Boolean.TRUE.equals(req.getSaveCard())) {
                var user = userService.getUserById(req.getUserId());
                // en Culqi la card id viene en charge.card.id o guardar desde el token; aquí asumimos req.cardToken es card_id
                user.setCardToken(req.getCardToken());
                userService.updateUser(req.getUserId(), user);
            }

            return ResponseEntity.ok(t);
        } catch (Exception e) {
            log.error("Error al cobrar: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error procesando pago: " + e.getMessage());
        }
    }
}
