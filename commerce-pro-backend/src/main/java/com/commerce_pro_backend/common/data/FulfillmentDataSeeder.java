package com.commerce_pro_backend.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FulfillmentDataSeeder {

    private final SeedDataLoader seedDataLoader;

    @Bean
    @Profile("dev")
    @Order(6)
    CommandLineRunner seedFulfillment(
            com.commerce_pro_backend.fulfillment.service.FulfillmentService fulfillmentService,
            com.commerce_pro_backend.fulfillment.service.ShipmentService shipmentService,
            com.commerce_pro_backend.order.repository.OrderRepository orderRepository,
            UserDetailsService userDetailsService) {
        return args -> {
            log.info("━━━ [Seed] Fulfillment ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            try {
                seedDataLoader.authenticateAsSuperAdmin(userDetailsService);

                // Carriers
                JsonNode carriersJson = seedDataLoader.loadTree("seed/carriers.json");
                com.commerce_pro_backend.fulfillment.dto.CarrierDTO manualCarrier = null;

                for (JsonNode c : carriersJson) {
                    com.commerce_pro_backend.fulfillment.dto.CarrierRequestDTO req =
                            new com.commerce_pro_backend.fulfillment.dto.CarrierRequestDTO();
                    req.setName(c.get("name").asText());
                    req.setCode(c.get("code").asText());
                    if (c.has("trackingUrlTemplate"))
                        req.setTrackingUrlTemplate(c.get("trackingUrlTemplate").asText());
                    if (c.has("notes")) req.setNotes(c.get("notes").asText());
                    req.setIsDefault(c.get("isDefault").asBoolean());

                    var carrier = fulfillmentService.createCarrier(req);
                    if (c.get("isDefault").asBoolean()) manualCarrier = carrier;
                }
                log.info("[Seed] Created {} carriers", carriersJson.size());

                // Shipping Rules
                JsonNode rulesJson = seedDataLoader.loadTree("seed/shipping-rules.json");
                for (JsonNode r : rulesJson) {
                    com.commerce_pro_backend.fulfillment.dto.ShippingRuleRequestDTO rule =
                            new com.commerce_pro_backend.fulfillment.dto.ShippingRuleRequestDTO();
                    rule.setName(r.get("name").asText());
                    rule.setConditionType(r.get("conditionType").asText());
                    if (r.has("conditionMin"))
                        rule.setConditionMin(new BigDecimal(r.get("conditionMin").asText()));
                    rule.setShippingMethod(r.get("shippingMethod").asText());
                    rule.setServiceLevel(r.get("serviceLevel").asText());
                    rule.setPriority(r.get("priority").asInt());
                    rule.setIsActive(r.get("isActive").asBoolean());
                    fulfillmentService.createRule(rule);
                }
                log.info("[Seed] Created {} shipping rules", rulesJson.size());

                // Pick List from CONFIRMED orders
                java.util.List<com.commerce_pro_backend.order.entity.Order> confirmedOrders =
                        orderRepository.findByStatus(com.commerce_pro_backend.order.enums.OrderStatus.CONFIRMED);
                if (!confirmedOrders.isEmpty()) {
                    java.util.List<String> pickOrderIds = confirmedOrders.stream()
                            .limit(2)
                            .map(com.commerce_pro_backend.order.entity.Order::getId)
                            .collect(Collectors.toList());

                    com.commerce_pro_backend.fulfillment.dto.CreatePickListRequest pickReq =
                            new com.commerce_pro_backend.fulfillment.dto.CreatePickListRequest();
                    pickReq.setOrderIds(pickOrderIds);
                    pickReq.setWarehouseName("Main Warehouse");
                    pickReq.setNotes("Seeded pick list for demo purposes");
                    var pickList = fulfillmentService.generatePickList(pickReq);
                    log.info("[Seed] Generated pick list {} for {} orders",
                            pickList.getPickListNumber(), pickOrderIds.size());
                }

                // Shipment from SHIPPED orders
                java.util.List<com.commerce_pro_backend.order.entity.Order> shippedOrders =
                        orderRepository.findByStatus(com.commerce_pro_backend.order.enums.OrderStatus.SHIPPED);
                if (!shippedOrders.isEmpty() && manualCarrier != null) {
                    com.commerce_pro_backend.order.entity.Order shippedOrder = shippedOrders.get(0);
                    com.commerce_pro_backend.fulfillment.dto.CreateShipmentRequest shipReq =
                            new com.commerce_pro_backend.fulfillment.dto.CreateShipmentRequest();
                    shipReq.setOrderId(shippedOrder.getId());
                    shipReq.setCarrierId(manualCarrier.getId());
                    shipReq.setTrackingNumber("TRK-DEMO-001");
                    shipReq.setShippingMethod("Standard");
                    shipReq.setEstimatedDeliveryDate(java.time.LocalDate.now().plusDays(3));
                    shipReq.setNotes("Seeded shipment for demo purposes");
                    var shipment = shipmentService.createShipment(shipReq);

                    com.commerce_pro_backend.fulfillment.dto.AddTrackingEventRequest eventReq =
                            new com.commerce_pro_backend.fulfillment.dto.AddTrackingEventRequest();
                    eventReq.setStatus("IN_TRANSIT");
                    eventReq.setDescription("Package departed sorting facility");
                    eventReq.setLocation("Distribution Center, Chicago IL");
                    shipmentService.addTrackingEvent(shipment.getId(), eventReq);
                    log.info("[Seed] Created shipment {} with tracking event", shipment.getShipmentNumber());
                }

                log.info("[Seed] Fulfillment seed complete");

            } catch (Exception e) {
                log.error("[Seed] Fulfillment seed failed: {}", e.getMessage(), e);
            } finally {
                seedDataLoader.clearSecurityContext();
            }
        };
    }
}
