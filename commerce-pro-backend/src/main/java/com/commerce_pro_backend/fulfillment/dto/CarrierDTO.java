package com.commerce_pro_backend.fulfillment.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CarrierDTO {
    private String id;
    private String name;
    private String code;
    private String status;
    private String trackingUrlTemplate;
    private String logoUrl;
    private Boolean isDefault;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
