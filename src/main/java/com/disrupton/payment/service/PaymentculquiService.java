package com.disrupton.payment.service;

import com.disrupton.config.CulquiConfig;
import com.disrupton.payment.dto.CulquiPaymentRequest;
import com.disrupton.payment.model.payment;
import com.disrupton.product.model.product;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class PaymentculquiService {

    private final CulquiConfig culquiConfig;
    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();
    private static final String COLLECTION = "paymentes";

    public PaymentculquiService(CulquiConfig culquiConfig) {
        this.culquiConfig = culquiConfig;
        this.httpClient = new OkHttpClient();
    }

    /**
     * Crea un cargo único (S/1, S/15 o precio de producto según paymentType).
     * Guarda la transacción en Firestore.
     */
    public payment cobrar(CulquiPaymentRequest req) throws Exception {
        long amount = determineAmount(req); // en céntimos

        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", amount);
        payload.put("currency_code", "PEN");
        payload.put("source_id", req.getCardToken()); // token/card_id
        payload.put("email", req.getEmail());
        payload.put("description", buildDescription(req));

        // Crear request a culqui Charges
        RequestBody body = RequestBody.create(
                MediaType.get("application/json; charset=utf-8"),
                gson.toJson(payload)
        );

        Request request = new Request.Builder()
                .url(culquiConfig.getApiUrl() + "/charges")
                .header("Authorization", "Bearer " + culquiConfig.getApiKey())
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            log.info("culqui response: {}", respBody);

            payment t = new payment();
            t.setId(UUID.randomUUID().toString());
            t.setUserId(req.getUserId());
            t.setAgenteId(req.getAgenteId());
            t.setTipo(mapPaymentType(req.getPaymentType()));
            t.setMonto(amount / 100.0);
            t.setTimestamp(Instant.now().toString());

            // Caso especial: pago por producto
            if ("PRODUCTO".equalsIgnoreCase(req.getPaymentType())) {
                t.setProductoId(req.getProductId());
                Firestore db = FirestoreClient.getFirestore();
                DocumentSnapshot doc = db.collection("products").document(req.getProductId()).get().get();
                if (doc.exists()) {
                    product p = doc.toObject(product.class);
                    if (p != null) {
                        t.setAgenteId(p.getCulturalAgentId());
                    }
                }
            }

            if (!response.isSuccessful()) {
                t.setEstado("RECHAZADO");
                saveTransaction(t);
                throw new RuntimeException("culqui error: " + respBody);
            }

            // Parsear respuesta de Culqi (charges no viene en "data")
            Map<?, ?> respMap = gson.fromJson(respBody, Map.class);
            String chargeId = respMap.get("id") != null ? respMap.get("id").toString() : null;
            String status = respMap.get("outcome") != null
                    ? ((Map<?, ?>) respMap.get("outcome")).get("type").toString()
                    : "unknown";

            t.setCulquiChargeId(chargeId);
            t.setEstado("venta_exitosa".equalsIgnoreCase(status) || "autorizado".equalsIgnoreCase(status)
                    ? "APROBADO"
                    : status.toUpperCase());

            saveTransaction(t);
            return t;
        }
    }

    private void saveTransaction(payment t) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        db.collection(COLLECTION).document(t.getId()).set(t);
        log.info("Transacción guardada id={}", t.getId());
    }

    private long determineAmount(CulquiPaymentRequest req) throws Exception {
        if (req.getPaymentType() == null) return 100L;

        switch (req.getPaymentType().toUpperCase()) {
            case "MENSUAL":
            case "SUSCRIPCION":
                return 1500L; // S/15.00
            case "PRODUCTO":
                Firestore db = FirestoreClient.getFirestore();
                DocumentSnapshot doc = db.collection("products")
                        .document(req.getProductId())
                        .get()
                        .get();
                if (!doc.exists()) throw new RuntimeException("Producto no encontrado");
                product p = doc.toObject(product.class);
                if (p == null || !p.isAvailable()) throw new RuntimeException("Producto no disponible");
                return Math.round(p.getPrice() * 100); // soles → céntimos
            case "POR_CONTACTO":
            case "UNICO":
            default:
                return 100L; // S/1.00
        }
    }

    private String buildDescription(CulquiPaymentRequest req) {
        if (req.getPaymentType() == null) return "Pago no especificado";

        switch (req.getPaymentType().toUpperCase()) {
            case "PRODUCTO":
                return "Compra de producto " + req.getProductId();
            case "MENSUAL":
            case "SUSCRIPCION":
                return "Suscripción mensual - usuario " + req.getUserId();
            case "POR_CONTACTO":
            case "UNICO":
            default:
                return "Acceso a contacto agente " + req.getAgenteId();
        }
    }

    private String mapPaymentType(String p) {
        if (p == null) return "UNICO";
        switch (p.toUpperCase()) {
            case "MENSUAL":
            case "SUSCRIPCION":
                return "SUSCRIPCION";
            case "ONE_CLICK":
                return "ONE_CLICK";
            case "PRODUCTO":
                return "PRODUCTO";
            case "POR_CONTACTO":
            default:
                return "UNICO";
        }
    }

}