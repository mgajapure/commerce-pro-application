package com.commerce_pro_backend.payment.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.payment.dto.request.CreatePayoutRequest;
import com.commerce_pro_backend.payment.dto.response.PayoutDTO;
import com.commerce_pro_backend.payment.dto.response.PayoutSummaryDTO;
import com.commerce_pro_backend.payment.enums.PayoutStatus;
import com.commerce_pro_backend.payment.service.PayoutService;
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

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/payments/payouts")
@RequiredArgsConstructor
@Tag(name = "Payouts", description = "Merchant payouts to bank — batch creation, approval, and bank transfer lifecycle")
public class PayoutController {

    private final PayoutService service;

    @GetMapping
    @Operation(summary = "List payouts", description = "Optionally filter by status")
    public ResponseEntity<ApiResponse<PageResponse<PayoutSummaryDTO>>> list(
            @RequestParam(required = false) PayoutStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Payouts retrieved", service.list(status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payout detail including all line items")
    public ResponseEntity<ApiResponse<PayoutDTO>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Payout retrieved", service.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new payout batch",
               description = "Collects all settled transactions in the given period that have not yet been paid out")
    public ResponseEntity<ApiResponse<PayoutDTO>> create(@Valid @RequestBody CreatePayoutRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payout created", service.create(req)));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a payout for processing")
    public ResponseEntity<ApiResponse<PayoutDTO>> approve(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Payout approved", service.approve(id)));
    }

    @PostMapping("/{id}/initiate")
    @Operation(summary = "Initiate the bank transfer for an approved payout")
    public ResponseEntity<ApiResponse<PayoutDTO>> initiate(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Payout initiated", service.initiate(id)));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Mark a payout as completed (funds confirmed received)")
    public ResponseEntity<ApiResponse<PayoutDTO>> complete(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Payout completed", service.complete(id)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a payout before it has been initiated")
    public ResponseEntity<ApiResponse<PayoutDTO>> cancel(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success("Payout cancelled", service.cancel(id, body.get("reason"))));
    }
}
