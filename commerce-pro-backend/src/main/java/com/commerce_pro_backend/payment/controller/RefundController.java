package com.commerce_pro_backend.payment.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.payment.dto.request.CreateRefundRequest;
import com.commerce_pro_backend.payment.dto.request.ReviewRefundRequest;
import com.commerce_pro_backend.payment.dto.response.RefundRequestDTO;
import com.commerce_pro_backend.payment.dto.response.RefundRequestSummaryDTO;
import com.commerce_pro_backend.payment.enums.RefundStatus;
import com.commerce_pro_backend.payment.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/payments/refunds")
@RequiredArgsConstructor
@Tag(name = "Refunds", description = "Refund request lifecycle — submit, approve, reject, process")
public class RefundController {

    private final RefundService service;

    @GetMapping
    @Operation(summary = "List refund requests", description = "Filter by status and/or order ID")
    public ResponseEntity<ApiResponse<PageResponse<RefundRequestSummaryDTO>>> list(
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(required = false) String orderId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Refund requests retrieved",
                service.list(status, orderId, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get refund request detail")
    public ResponseEntity<ApiResponse<RefundRequestDTO>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Refund request retrieved", service.getById(id)));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get all refund requests for an order")
    public ResponseEntity<ApiResponse<List<RefundRequestSummaryDTO>>> getByOrderId(@PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.success("Order refunds retrieved", service.getByOrderId(orderId)));
    }

    @PostMapping
    @Operation(summary = "Submit a refund request",
               description = "Creates a refund request in PENDING status. Requires separate approval before processing.")
    public ResponseEntity<ApiResponse<RefundRequestDTO>> request(@Valid @RequestBody CreateRefundRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Refund request submitted", service.requestRefund(req)));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a refund request", description = "Moves the request to APPROVED — ready for gateway processing")
    public ResponseEntity<ApiResponse<RefundRequestDTO>> approve(
            @PathVariable String id,
            @RequestBody ReviewRefundRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Refund request approved", service.approve(id, req)));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a refund request")
    public ResponseEntity<ApiResponse<RefundRequestDTO>> reject(
            @PathVariable String id,
            @RequestBody ReviewRefundRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Refund request rejected", service.reject(id, req)));
    }

    @PostMapping("/{id}/process")
    @Operation(summary = "Process an approved refund on the payment gateway",
               description = "Submits the refund to the gateway. Updates Order.refundedAmount and Order.paymentStatus on success.")
    public ResponseEntity<ApiResponse<RefundRequestDTO>> process(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Refund processed", service.process(id)));
    }
}
