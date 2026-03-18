package com.commerce_pro_backend.fulfillment.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ShipmentFilterDTO {
    private String search;
    private String status;
    private String carrierId;
    private String orderId;
    private String createdFrom;
    private String createdTo;
}
