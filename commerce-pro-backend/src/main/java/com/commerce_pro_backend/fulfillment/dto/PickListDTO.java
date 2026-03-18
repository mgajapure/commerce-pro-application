package com.commerce_pro_backend.fulfillment.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PickListDTO {
    private String id;
    private String pickListNumber;
    private String type;
    private String status;
    private String warehouseId;
    private String warehouseName;
    private String assignedToUserId;
    private String assignedToName;
    private int totalOrders;
    private int totalItems;
    private int completedItems;
    private String notes;
    private LocalDateTime generatedAt;
    private LocalDateTime assignedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private List<PickListItemDTO> items;
    // Computed
    private int pendingItems;
    private double completionPercentage;
}
