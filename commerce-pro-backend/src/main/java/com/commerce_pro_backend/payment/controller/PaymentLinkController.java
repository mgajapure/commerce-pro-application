package com.commerce_pro_backend.payment.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.payment.dto.request.CreatePaymentLinkRequest;
import com.commerce_pro_backend.payment.dto.response.PaymentLinkDTO;
import com.commerce_pro_backend.payment.dto.response.PaymentLinkSummaryDTO;
import com.commerce_pro_backend.payment.enums.PaymentLinkStatus;
import com.commerce_pro_backend.payment.service.PaymentLinkService;
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

@Slf4j
@RestController
@RequestMapping("/v1/payments/links")
@RequiredArgsConstructor
@Tag(name = "Payment Links", description = "Shareable payment URLs — one-time and reusable, with expiry and usage limits")
public class PaymentLinkController {

    private final PaymentLinkService service;

    @GetMapping
    @Operation(summary = "List payment links", description = "Optionally filter by status")
    public ResponseEntity<ApiResponse<PageResponse<PaymentLinkSummaryDTO>>> list(
            @RequestParam(required = false) PaymentLinkStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Payment links retrieved", service.list(status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment link detail")
    public ResponseEntity<ApiResponse<PaymentLinkDTO>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Payment link retrieved", service.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new payment link",
               description = "Generates a shareable URL. Can be tied to an order or standalone. Supports one-time and reusable modes.")
    public ResponseEntity<ApiResponse<PaymentLinkDTO>> create(@Valid @RequestBody CreatePaymentLinkRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment link created", service.create(req)));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a payment link", description = "Cancelled links can no longer be paid")
    public ResponseEntity<ApiResponse<PaymentLinkDTO>> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Payment link deactivated", service.deactivate(id)));
    }

    // ── Public endpoints — no authentication required ─────────────────────────

    @GetMapping("/slug/{slug}")
    @Operation(summary = "[PUBLIC] Resolve a payment link slug",
               description = "No authentication required. Used by the customer-facing payment page to display link details.")
    public ResponseEntity<ApiResponse<PaymentLinkDTO>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success("Payment link resolved", service.getBySlug(slug)));
    }
}
