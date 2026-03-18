package com.commerce_pro_backend.fulfillment.dto;

import lombok.*;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FulfillmentStatsDTO {
    private long queueSize;           // orders ready to fulfill
    private long generatedPickLists;
    private long inProgressPickLists;
    private long completedPickListsToday;
    private long totalShipments;
    private long inTransitShipments;
    private long deliveredShipments;
    private long overdueShipments;
    private long exceptionShipments;
    private Map<String, Long> pickListStatusBreakdown;
    private Map<String, Long> shipmentStatusBreakdown;
}
