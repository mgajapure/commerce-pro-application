package com.commerce_pro_backend.fulfillment.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.fulfillment.dto.*;
import com.commerce_pro_backend.fulfillment.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/shipments")
@RequiredArgsConstructor
@Tag(name = "Shipments", description = "Shipment creation, tracking, and delivery management API")
public class ShipmentController {

    private final ShipmentService shipmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('fulfillment:shipment:read')")
    @Operation(summary = "List shipments", description = "Paginated, filterable list of all shipments")
    public ResponseEntity<ApiResponse<PageResponse<ShipmentSummaryDTO>>> listShipments(
            @ParameterObject @ModelAttribute ShipmentFilterDTO filter,
            @PageableDefault(size = 20, sort = "createdAt") @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Shipments retrieved",
                shipmentService.listShipments(filter, pageable)));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('fulfillment:shipment:read')")
    @Operation(summary = "Shipment statistics")
    public ResponseEntity<ApiResponse<ShipmentStatsDTO>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Shipment stats retrieved", shipmentService.getStats()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('fulfillment:shipment:read')")
    @Operation(summary = "Get shipment by ID")
    public ResponseEntity<ApiResponse<ShipmentDTO>> getShipment(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Shipment retrieved", shipmentService.getShipment(id)));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAuthority('fulfillment:shipment:read')")
    @Operation(summary = "Get shipments by order", description = "All shipments associated with a given order")
    public ResponseEntity<ApiResponse<List<ShipmentSummaryDTO>>> getByOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.success("Shipments retrieved",
                shipmentService.getShipmentsByOrder(orderId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('fulfillment:shipment:create')")
    @Operation(summary = "Create shipment", description = "Create a shipment for an order; syncs tracking info back to the order")
    public ResponseEntity<ApiResponse<ShipmentDTO>> createShipment(
            @Valid @RequestBody CreateShipmentRequest request) {
        ShipmentDTO created = shipmentService.createShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shipment created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('fulfillment:shipment:update')")
    @Operation(summary = "Update shipment", description = "Update carrier, tracking number, dimensions, cost etc.")
    public ResponseEntity<ApiResponse<ShipmentDTO>> updateShipment(
            @PathVariable String id,
            @Valid @RequestBody UpdateShipmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Shipment updated",
                shipmentService.updateShipment(id, request)));
    }

    @PostMapping("/{id}/tracking-events")
    @PreAuthorize("hasAuthority('fulfillment:shipment:update')")
    @Operation(summary = "Add tracking event", description = "Append a tracking event — automatically advances shipment status and syncs to order")
    public ResponseEntity<ApiResponse<ShipmentDTO>> addTrackingEvent(
            @PathVariable String id,
            @Valid @RequestBody AddTrackingEventRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tracking event added",
                shipmentService.addTrackingEvent(id, request)));
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasAuthority('fulfillment:shipment:manage')")
    @Operation(summary = "Mark delivered", description = "Shortcut to mark a shipment as DELIVERED and transition the order to DELIVERED")
    public ResponseEntity<ApiResponse<ShipmentDTO>> markDelivered(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Shipment marked as delivered",
                shipmentService.markDelivered(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('fulfillment:shipment:delete')")
    @Operation(summary = "Delete shipment", description = "Void a LABEL_CREATED shipment. Cannot delete shipments already picked up.")
    public ResponseEntity<ApiResponse<Void>> deleteShipment(@PathVariable String id) {
        shipmentService.deleteShipment(id);
        return ResponseEntity.ok(ApiResponse.success("Shipment deleted", null));
    }
}
