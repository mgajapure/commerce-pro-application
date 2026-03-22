package com.commerce_pro_backend.payment.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.payment.dto.response.PaymentTransactionDTO;
import com.commerce_pro_backend.payment.dto.response.PaymentTransactionSummaryDTO;
import com.commerce_pro_backend.payment.service.PaymentTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/payments/pending")
@RequiredArgsConstructor
@Tag(name = "Pending Payments", description = "Manage payments awaiting capture — authorizations, retries, expiries")
public class PendingPaymentController {

    private final PaymentTransactionService service;

    @GetMapping
    @Operation(summary = "List all pending / authorized payments")
    public ResponseEntity<ApiResponse<List<PaymentTransactionSummaryDTO>>> listPending(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Pending payments retrieved", service.listPending(pageable)));
    }

    @GetMapping("/expiring")
    @Operation(summary = "List authorizations expiring within 24 hours")
    public ResponseEntity<ApiResponse<List<PaymentTransactionSummaryDTO>>> listExpiring() {
        return ResponseEntity.ok(ApiResponse.success("Expiring authorizations retrieved", service.listExpiring()));
    }

    @PostMapping("/{id}/capture")
    @Operation(summary = "Capture a pending authorization")
    public ResponseEntity<ApiResponse<PaymentTransactionDTO>> capture(
            @PathVariable String id,
            @RequestParam(required = false) BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success("Payment captured", service.capture(id, amount)));
    }

    @PostMapping("/{id}/void")
    @Operation(summary = "Void a pending authorization")
    public ResponseEntity<ApiResponse<PaymentTransactionDTO>> voidAuth(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Authorization voided", service.voidTransaction(id)));
    }
}
