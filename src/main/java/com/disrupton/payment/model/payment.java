package com.disrupton.payment.model;


import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Data;

@Data
public class payment {

    @DocumentId
    private String id;
    private String userId;
    private String agenteId; // null si es suscripción
    private String tipo; // "UNICO", "ONE_CLICK", "SUSCRIPCION"
    private double monto;
    private String currency = "PEN";
    private String culquiChargeId; // id de cargo en Culqi
    private String estado;
    private String timestamp; // ISO string
    private String productoId;
}
