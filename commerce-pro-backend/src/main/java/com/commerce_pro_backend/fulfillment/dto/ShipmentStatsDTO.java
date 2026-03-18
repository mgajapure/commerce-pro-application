package com.commerce_pro_backend.fulfillment.dto;

import lombok.*;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ShipmentStatsDTO {
    private long totalShipments;
    private long shipmentsToday;
    private long inTransit;
    private long outForDelivery;
    private long delivered;
    private long exceptions;
    private long overdue;
    private Map<String, Long> statusBreakdown;
}
