package com.commerce_pro_backend.fulfillment.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class PickListFilterDTO {
    private String search;
    private String status;
    private String type;
    private String assignedToUserId;
    private String warehouseId;
    private String createdFrom;
    private String createdTo;
}
