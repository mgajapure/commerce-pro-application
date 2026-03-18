package com.commerce_pro_backend.fulfillment.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.fulfillment.dto.*;
import com.commerce_pro_backend.fulfillment.service.FulfillmentService;
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
@RequestMapping("/v1/fulfillment")
@RequiredArgsConstructor
@Tag(name = "Fulfillment", description = "Pick-list, fulfillment queue, carriers, and shipping rules API")
public class FulfillmentController {

    private final FulfillmentService fulfillmentService;

    // ── Queue ──────────────────────────────────────────────────────────────────

    @GetMapping("/queue")
    @PreAuthorize("hasAuthority('fulfillment:picklist:read')")
    @Operation(summary = "Fulfillment queue", description = "Orders ready to pick — CONFIRMED/PROCESSING with unfulfilled items")
    public ResponseEntity<ApiResponse<List<FulfillmentQueueItemDTO>>> getQueue() {
        return ResponseEntity.ok(ApiResponse.success("Fulfillment queue retrieved", fulfillmentService.getQueue()));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('fulfillment:stats')")
    @Operation(summary = "Fulfillment statistics")
    public ResponseEntity<ApiResponse<FulfillmentStatsDTO>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Fulfillment stats retrieved", fulfillmentService.getStats()));
    }

    // ── Pick Lists ─────────────────────────────────────────────────────────────

    @GetMapping("/pick-lists")
    @PreAuthorize("hasAuthority('fulfillment:picklist:read')")
    @Operation(summary = "List pick lists", description = "Paginated, filterable list of all pick lists")
    public ResponseEntity<ApiResponse<PageResponse<PickListSummaryDTO>>> listPickLists(
            @ParameterObject @ModelAttribute PickListFilterDTO filter,
            @PageableDefault(size = 20, sort = "createdAt") @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Pick lists retrieved",
                fulfillmentService.listPickLists(filter, pageable)));
    }

    @PostMapping("/pick-lists")
    @PreAuthorize("hasAuthority('fulfillment:picklist:create')")
    @Operation(summary = "Generate pick list", description = "Create a pick list from one or more confirmed/processing orders")
    public ResponseEntity<ApiResponse<PickListDTO>> generatePickList(
            @Valid @RequestBody CreatePickListRequest request) {
        PickListDTO created = fulfillmentService.generatePickList(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Pick list generated successfully", created));
    }

    @GetMapping("/pick-lists/{id}")
    @PreAuthorize("hasAuthority('fulfillment:picklist:read')")
    @Operation(summary = "Get pick list by ID")
    public ResponseEntity<ApiResponse<PickListDTO>> getPickList(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Pick list retrieved", fulfillmentService.getPickList(id)));
    }

    @PostMapping("/pick-lists/{id}/assign")
    @PreAuthorize("hasAuthority('fulfillment:picklist:manage')")
    @Operation(summary = "Assign pick list", description = "Assign a GENERATED pick list to a warehouse worker")
    public ResponseEntity<ApiResponse<PickListDTO>> assignPickList(
            @PathVariable String id,
            @Valid @RequestBody AssignPickListRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Pick list assigned",
                fulfillmentService.assignPickList(id, request)));
    }

    @PostMapping("/pick-lists/{id}/start")
    @PreAuthorize("hasAuthority('fulfillment:picklist:manage')")
    @Operation(summary = "Start pick list", description = "Mark pick list as IN_PROGRESS")
    public ResponseEntity<ApiResponse<PickListDTO>> startPickList(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Pick list started",
                fulfillmentService.startPickList(id)));
    }

    @PatchMapping("/pick-lists/{id}/items/{itemId}")
    @PreAuthorize("hasAuthority('fulfillment:picklist:manage')")
    @Operation(summary = "Update pick item", description = "Record quantity picked for a single pick list item")
    public ResponseEntity<ApiResponse<PickListItemDTO>> updatePickItem(
            @PathVariable String id,
            @PathVariable String itemId,
            @Valid @RequestBody UpdatePickItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Pick item updated",
                fulfillmentService.updatePickItem(id, itemId, request)));
    }

    @PostMapping("/pick-lists/{id}/complete")
    @PreAuthorize("hasAuthority('fulfillment:picklist:manage')")
    @Operation(summary = "Complete pick list", description = "Mark all items as picked — updates inventory and order fulfillment status")
    public ResponseEntity<ApiResponse<PickListDTO>> completePickList(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Pick list completed",
                fulfillmentService.completePickList(id)));
    }

    @PostMapping("/pick-lists/{id}/cancel")
    @PreAuthorize("hasAuthority('fulfillment:picklist:manage')")
    @Operation(summary = "Cancel pick list", description = "Cancel a GENERATED or ASSIGNED pick list")
    public ResponseEntity<ApiResponse<PickListDTO>> cancelPickList(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Pick list cancelled",
                fulfillmentService.cancelPickList(id)));
    }

    // ── Carriers ───────────────────────────────────────────────────────────────

    @GetMapping("/carriers")
    @PreAuthorize("hasAuthority('fulfillment:carrier:read')")
    @Operation(summary = "List carriers")
    public ResponseEntity<ApiResponse<List<CarrierDTO>>> listCarriers() {
        return ResponseEntity.ok(ApiResponse.success("Carriers retrieved", fulfillmentService.listCarriers()));
    }

    @PostMapping("/carriers")
    @PreAuthorize("hasAuthority('fulfillment:carrier:manage')")
    @Operation(summary = "Create carrier")
    public ResponseEntity<ApiResponse<CarrierDTO>> createCarrier(
            @Valid @RequestBody CarrierRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Carrier created", fulfillmentService.createCarrier(request)));
    }

    @PutMapping("/carriers/{id}")
    @PreAuthorize("hasAuthority('fulfillment:carrier:manage')")
    @Operation(summary = "Update carrier")
    public ResponseEntity<ApiResponse<CarrierDTO>> updateCarrier(
            @PathVariable String id,
            @Valid @RequestBody CarrierRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success("Carrier updated", fulfillmentService.updateCarrier(id, request)));
    }

    @DeleteMapping("/carriers/{id}")
    @PreAuthorize("hasAuthority('fulfillment:carrier:manage')")
    @Operation(summary = "Delete carrier")
    public ResponseEntity<ApiResponse<Void>> deleteCarrier(@PathVariable String id) {
        fulfillmentService.deleteCarrier(id);
        return ResponseEntity.ok(ApiResponse.success("Carrier deleted", null));
    }

    // ── Shipping Rules ─────────────────────────────────────────────────────────

    @GetMapping("/shipping-rules")
    @PreAuthorize("hasAuthority('fulfillment:rules:read')")
    @Operation(summary = "List shipping rules")
    public ResponseEntity<ApiResponse<List<ShippingRuleDTO>>> listRules() {
        return ResponseEntity.ok(ApiResponse.success("Shipping rules retrieved", fulfillmentService.listRules()));
    }

    @PostMapping("/shipping-rules")
    @PreAuthorize("hasAuthority('fulfillment:rules:manage')")
    @Operation(summary = "Create shipping rule")
    public ResponseEntity<ApiResponse<ShippingRuleDTO>> createRule(
            @Valid @RequestBody ShippingRuleRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shipping rule created", fulfillmentService.createRule(request)));
    }

    @PutMapping("/shipping-rules/{id}")
    @PreAuthorize("hasAuthority('fulfillment:rules:manage')")
    @Operation(summary = "Update shipping rule")
    public ResponseEntity<ApiResponse<ShippingRuleDTO>> updateRule(
            @PathVariable String id,
            @Valid @RequestBody ShippingRuleRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success("Shipping rule updated", fulfillmentService.updateRule(id, request)));
    }

    @DeleteMapping("/shipping-rules/{id}")
    @PreAuthorize("hasAuthority('fulfillment:rules:manage')")
    @Operation(summary = "Delete shipping rule")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable String id) {
        fulfillmentService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success("Shipping rule deleted", null));
    }
}
