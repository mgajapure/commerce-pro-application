package com.commerce_pro_backend.fulfillment.dto;

import lombok.*;
import java.time.LocalDateTime;

/** Lightweight pick list used in paginated lists. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PickListSummaryDTO {
    private String id;
    private String pickListNumber;
    private String type;
    private String status;
    private String warehouseName;
    private String assignedToName;
    private int totalOrders;
    private int totalItems;
    private int completedItems;
    private double completionPercentage;
    private LocalDateTime generatedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
