package com.disrupton.user.dto;

import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id; // ID del documento en Firestore
    private String fullName;
    private String email;
    private String role; // student, moderator, admin
    private Timestamp createdAt;
    private String tarjetaToken; // para one-click (card id)
    private Date suscripcionExpira; // fecha expiracion
    private Boolean isActive;
    private List<String> contactedProviderIds;
    private List<String> productIds;
    private String region;
    private String numberPhone;
    private String subscriptionType;
    private boolean active;
    private String CardToken;

}