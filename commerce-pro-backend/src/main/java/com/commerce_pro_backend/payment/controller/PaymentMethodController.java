package com.commerce_pro_backend.payment.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.payment.dto.request.AddPaymentMethodRequest;
import com.commerce_pro_backend.payment.dto.response.PaymentMethodDTO;
import com.commerce_pro_backend.payment.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/payments/methods")
@RequiredArgsConstructor
@Tag(name = "Payment Methods", description = "Customer saved payment methods — tokenised cards and wallets")
public class PaymentMethodController {

    private final PaymentMethodService service;

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all active saved payment methods for a customer")
    public ResponseEntity<ApiResponse<List<PaymentMethodDTO>>> getByCustomerId(@PathVariable String customerId) {
        return ResponseEntity.ok(ApiResponse.success("Payment methods retrieved", service.getByCustomerId(customerId)));
    }

    @PostMapping
    @Operation(summary = "Add a tokenised payment method for a customer",
               description = "Stores the gateway token only — raw card numbers are never persisted")
    public ResponseEntity<ApiResponse<PaymentMethodDTO>> add(@Valid @RequestBody AddPaymentMethodRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment method added", service.add(req)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete (deactivate) a saved payment method")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable String id) {
        service.remove(id);
        return ResponseEntity.ok(ApiResponse.success("Payment method removed", null));
    }

    @PostMapping("/{id}/set-default")
    @Operation(summary = "Set a payment method as the customer's default")
    public ResponseEntity<ApiResponse<PaymentMethodDTO>> setDefault(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Default payment method updated", service.setDefault(id)));
    }
}
