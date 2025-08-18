package com.disrupton.user.model;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @DocumentId
    private Long id;
    private String CardToken;
    private String username;
    private String email;
    private String fullName;
    private String region; 
    private String role; 
    private LocalDateTime createdAt;
    private Boolean isActive;
    private String numberPhone;
    private String subscriptionType;
    private List<String> contactedProviderIds; // Solo si es estudiante
    private List<String> ProductIds;  // Solo si es agente cultural
    private boolean active; // para soft delete o desactivación

    public enum Role {
        STUDENT, MODERATOR, ADMIN
    }
} 