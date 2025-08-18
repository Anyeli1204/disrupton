package com.disrupton.payment.dto;

import lombok.Data;

@Data
public class CulquiPaymentRequest {
    private String userId;        // ID del estudiante (Firestore doc ID)
    private String email;
    private String paymentType;   // "POR_CONTACTO" | "MENSUAL" | "ONE_CLICK"
    private String agenteId;      // id del proveedor cultural (solo para POR_CONTACTO)
    private String cardToken;     // token/card_id provisto por Culqi (pm-like)
    private Boolean saveCard;     // si true, guarda card_id en usuario para one-click
    private String productId;

}
