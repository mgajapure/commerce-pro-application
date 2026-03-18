package com.commerce_pro_backend.order.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.order.dto.*;
import com.commerce_pro_backend.order.service.OrderService;
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
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Full order lifecycle management API")
public class OrderController {

    private final OrderService orderService;

    // ── List & Search ─────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('order:order:read')")
    @Operation(summary = "List orders", description = "Paginated, filterable list of all orders")
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryDTO>>> listOrders(
            @ParameterObject @ModelAttribute OrderFilterDTO filter,
            @PageableDefault(size = 20, sort = "createdAt") @ParameterObject Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(
                "Orders retrieved successfully",
                orderService.listOrders(filter, pageable)));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('order:order:read')")
    @Operation(summary = "Order statistics", description = "Dashboard KPI stats for orders")
    public ResponseEntity<ApiResponse<OrderStatsDTO>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Order stats retrieved", orderService.getStats()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order:order:read')")
    @Operation(summary = "Get order by ID", description = "Full order detail including items and status history")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Order retrieved successfully",
                orderService.getOrderById(id)));
    }

    @GetMapping("/number/{orderNumber}")
    @PreAuthorize("hasAuthority('order:order:read')")
    @Operation(summary = "Get order by order number", description = "Look up an order by its human-readable reference number")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderByNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(
                "Order retrieved successfully",
                orderService.getOrderByNumber(orderNumber)));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAuthority('order:order:read')")
    @Operation(summary = "Get orders by customer", description = "Paginated list of all orders placed by a specific customer")
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryDTO>>> getCustomerOrders(
            @PathVariable String customerId,
            @PageableDefault(size = 20, sort = "createdAt") @ParameterObject Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(
                "Customer orders retrieved",
                orderService.getCustomerOrders(customerId, pageable)));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('order:order:create')")
    @Operation(summary = "Create order", description = "Manually create a new order (draft by default)")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> createOrder(
            @Valid @RequestBody CreateOrderRequestDTO request) {

        OrderResponseDTO created = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", created));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('order:order:update')")
    @Operation(summary = "Update order", description = "Update order details (only allowed before fulfillment)")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateOrder(
            @PathVariable String id,
            @Valid @RequestBody UpdateOrderRequestDTO request) {

        return ResponseEntity.ok(ApiResponse.success(
                "Order updated successfully",
                orderService.updateOrder(id, request)));
    }

    // ── Status Transitions ────────────────────────────────────────────────────

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('order:order:manage-status')")
    @Operation(summary = "Confirm order", description = "Confirm the order and reserve inventory. Transitions: DRAFT / PENDING_PAYMENT → CONFIRMED")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> confirmOrder(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Order confirmed", orderService.confirmOrder(id)));
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasAuthority('order:order:manage-status')")
    @Operation(summary = "Mark as processing", description = "Move order to PROCESSING. Transitions: CONFIRMED / ON_HOLD → PROCESSING")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> markProcessing(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Order moved to processing", orderService.markProcessing(id)));
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("hasAuthority('order:order:manage-status')")
    @Operation(summary = "Mark as shipped", description = "Mark order as shipped and attach tracking info. Transitions: PROCESSING / FULFILLED → SHIPPED")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> markShipped(
            @PathVariable String id,
            @Valid @RequestBody TrackingUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.success("Order marked as shipped", orderService.markShipped(id, request)));
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasAuthority('order:order:manage-status')")
    @Operation(summary = "Mark as delivered", description = "Mark order as delivered. Transitions: SHIPPED / OUT_FOR_DELIVERY → DELIVERED")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> markDelivered(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Order marked as delivered", orderService.markDelivered(id)));
    }

    @PostMapping("/{id}/hold")
    @PreAuthorize("hasAuthority('order:order:manage-status')")
    @Operation(summary = "Place order on hold", description = "Pause order processing with a required reason")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> holdOrder(
            @PathVariable String id,
            @Valid @RequestBody OrderHoldRequest request) {

        return ResponseEntity.ok(ApiResponse.success("Order placed on hold", orderService.holdOrder(id, request)));
    }

    @PostMapping("/{id}/release-hold")
    @PreAuthorize("hasAuthority('order:order:manage-status')")
    @Operation(summary = "Release order hold", description = "Release a hold and return order to CONFIRMED status")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> releaseHold(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Order hold released", orderService.releaseHold(id)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('order:order:manage-status')")
    @Operation(summary = "Close order", description = "Mark a delivered/refunded order as fully closed. Transitions: DELIVERED / REFUNDED → CLOSED")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> closeOrder(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Order closed", orderService.closeOrder(id)));
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('order:order:cancel')")
    @Operation(summary = "Cancel order", description = "Cancel an order (soft delete — inventory reservations released)")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> cancelOrder(
            @PathVariable String id,
            @Valid @RequestBody OrderCancelRequest request) {

        return ResponseEntity.ok(ApiResponse.success("Order cancelled", orderService.cancelOrder(id, request)));
    }

    // ── Tracking ──────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/tracking")
    @PreAuthorize("hasAuthority('order:order:update')")
    @Operation(summary = "Update tracking", description = "Attach or update tracking number and carrier for a shipped order")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateTracking(
            @PathVariable String id,
            @Valid @RequestBody TrackingUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.success("Tracking updated", orderService.updateTracking(id, request)));
    }

    // ── Bulk Actions ─────────────────────────────────────────────────────────

    @PostMapping("/bulk-action")
    @PreAuthorize("hasAuthority('order:order:bulk-action')")
    @Operation(summary = "Bulk status action", description = "Apply a status transition to multiple orders at once")
    public ResponseEntity<ApiResponse<List<String>>> bulkAction(
            @Valid @RequestBody BulkOrderActionRequest request) {

        List<String> failures = orderService.bulkAction(request);
        String message = failures.isEmpty()
                ? "Bulk action completed successfully"
                : "Bulk action completed with " + failures.size() + " failure(s)";
        return ResponseEntity.ok(ApiResponse.success(message, failures));
    }
}
