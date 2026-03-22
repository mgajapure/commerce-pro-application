package com.commerce_pro_backend.payment.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.payment.dto.request.CreatePaymentRequest;
import com.commerce_pro_backend.payment.dto.request.FlagTransactionRequest;
import com.commerce_pro_backend.payment.dto.response.PaymentStatsDTO;
import com.commerce_pro_backend.payment.dto.response.PaymentTransactionDTO;
import com.commerce_pro_backend.payment.dto.response.PaymentTransactionSummaryDTO;
import com.commerce_pro_backend.payment.enums.GatewayProvider;
import com.commerce_pro_backend.payment.enums.TransactionStatus;
import com.commerce_pro_backend.payment.enums.TransactionType;
import com.commerce_pro_backend.payment.service.PaymentTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/v1/payments/transactions")
@RequiredArgsConstructor
@Tag(name = "Payment Transactions", description = "Core payment ledger — charges, authorizations, captures, voids")
public class PaymentTransactionController {

    private final PaymentTransactionService service;

    @GetMapping
    @Operation(summary = "List all payment transactions", description = "Supports filtering by status, type, gateway, order, customer, flags, and date range")
    public ResponseEntity<ApiResponse<PageResponse<PaymentTransactionSummaryDTO>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) GatewayProvider gateway,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) Boolean isFlagged,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved",
                service.list(search, status, type, gateway, orderId, customerId, isFlagged, from, to, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<ApiResponse<PaymentTransactionDTO>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Transaction retrieved", service.getById(id)));
    }

    @GetMapping("/ref/{ref}")
    @Operation(summary = "Get transaction by reference number (PAY-YYYYMMDD-NNNNNN)")
    public ResponseEntity<ApiResponse<PaymentTransactionDTO>> getByRef(@PathVariable String ref) {
        return ResponseEntity.ok(ApiResponse.success("Transaction retrieved", service.getByRef(ref)));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get all transactions for an order")
    public ResponseEntity<ApiResponse<PageResponse<PaymentTransactionSummaryDTO>>> getByOrderId(
            @PathVariable String orderId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Order transactions retrieved",
                service.getByOrderId(orderId, pageable)));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get payment dashboard statistics")
    public ResponseEntity<ApiResponse<PaymentStatsDTO>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Payment stats retrieved", service.getStats()));
    }

    @PostMapping
    @Operation(summary = "Create and process a payment", description = "Supports CHARGE (immediate) and AUTHORIZE (capture later) transaction types")
    public ResponseEntity<ApiResponse<PaymentTransactionDTO>> create(
            @Valid @RequestBody CreatePaymentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment processed", service.createPayment(req)));
    }

    @PostMapping("/{id}/capture")
    @Operation(summary = "Capture an authorized payment", description = "Capture the full or partial amount from an AUTHORIZED transaction")
    public ResponseEntity<ApiResponse<PaymentTransactionDTO>> capture(
            @PathVariable String id,
            @RequestParam(required = false) BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success("Payment captured", service.capture(id, amount)));
    }

    @PostMapping("/{id}/void")
    @Operation(summary = "Void an authorized payment", description = "Cancel an authorization before it has been captured")
    public ResponseEntity<ApiResponse<PaymentTransactionDTO>> voidTransaction(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Payment voided", service.voidTransaction(id)));
    }

    @PatchMapping("/{id}/flag")
    @Operation(summary = "Flag or unflag a transaction for fraud review")
    public ResponseEntity<ApiResponse<PaymentTransactionDTO>> flag(
            @PathVariable String id,
            @RequestBody FlagTransactionRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Transaction flag updated", service.flagTransaction(id, req)));
    }
}
