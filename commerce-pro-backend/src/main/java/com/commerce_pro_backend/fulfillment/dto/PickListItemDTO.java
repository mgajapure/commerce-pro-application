package com.commerce_pro_backend.fulfillment.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PickListItemDTO {
    private String id;
    private String orderId;
    private String orderNumber;
    private String orderItemId;
    private String productId;
    private String sku;
    private String productName;
    private String variantInfo;
    private String binLocation;
    private int quantityRequired;
    private int quantityPicked;
    private boolean isPicked;
    private String notes;
    private LocalDateTime createdAt;
}
