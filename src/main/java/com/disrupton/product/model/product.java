package com.disrupton.product.model;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class product {

    @DocumentId
    private String id;
    private String culturalAgentId;
    private String name;
    private String description;
    private String imageUrl;
    private double price;
    private boolean available;
}

