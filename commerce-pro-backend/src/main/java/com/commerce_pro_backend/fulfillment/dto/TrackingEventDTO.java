package com.commerce_pro_backend.fulfillment.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TrackingEventDTO {
    private String id;
    private String status;
    private String location;
    private String description;
    private LocalDateTime eventTime;
    private LocalDateTime createdAt;
    private String createdBy;
}
