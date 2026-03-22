package com.commerce_pro_backend.payment.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.payment.config.PaymentCredentialEncryptor;
import com.commerce_pro_backend.payment.dto.request.CreateGatewayConfigRequest;
import com.commerce_pro_backend.payment.dto.response.PaymentGatewayConfigDTO;
import com.commerce_pro_backend.payment.entity.PaymentGatewayConfig;
import com.commerce_pro_backend.payment.mapper.PaymentMapper;
import com.commerce_pro_backend.payment.service.PaymentGatewayService;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.service.AuditService;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
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
@RequestMapping("/v1/payments/gateways")
@RequiredArgsConstructor
@Tag(name = "Payment Gateways", description = "Gateway configuration management — credentials stored encrypted with AES-256-GCM")
public class PaymentGatewayController {

    private final PaymentGatewayService gatewayService;
    private final PaymentMapper mapper;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @Operation(summary = "List all configured payment gateways",
               description = "API keys and secrets are intentionally excluded from all responses")
    public ResponseEntity<ApiResponse<List<PaymentGatewayConfigDTO>>> list() {
        return ResponseEntity.ok(ApiResponse.success("Gateways retrieved",
                mapper.toGatewayConfigDTOList(gatewayService.listAll())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get gateway configuration detail")
    public ResponseEntity<ApiResponse<PaymentGatewayConfigDTO>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Gateway retrieved",
                mapper.toGatewayConfigDTO(gatewayService.getById(id))));
    }

    @PostMapping
    @Operation(summary = "Add a new payment gateway configuration",
               description = "Credentials (apiKey, apiSecret, webhookSecret) are encrypted at rest using AES-256-GCM before storage")
    public ResponseEntity<ApiResponse<PaymentGatewayConfigDTO>> create(
            @Valid @RequestBody CreateGatewayConfigRequest req) {
        String actorId = currentUserService.getCurrentUserId();
        PaymentGatewayConfig config = PaymentGatewayConfig.builder()
                .gatewayProvider(req.getGatewayProvider())
                .displayName(req.getDisplayName())
                .apiKey(req.getApiKey())
                .apiSecret(req.getApiSecret())
                .webhookSecret(req.getWebhookSecret())
                .isTestMode(req.getIsTestMode() == null || req.getIsTestMode())
                .isActive(false)
                .isDefault(false)
                .supportedCurrencies(req.getSupportedCurrencies())
                .webhookUrl(req.getWebhookUrl())
                .configMetadata(req.getConfigMetadata())
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();
        PaymentGatewayConfig saved = gatewayService.save(config);
        auditService.log(actorId, AuditAction.GATEWAY_CONFIG_CREATED, "PaymentGatewayConfig",
                saved.getId(), saved.getGatewayProvider().name(), null, null, "CREATED", true);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gateway configuration created", mapper.toGatewayConfigDTO(saved)));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate a gateway configuration")
    public ResponseEntity<ApiResponse<PaymentGatewayConfigDTO>> activate(@PathVariable String id) {
        String actorId = currentUserService.getCurrentUserId();
        PaymentGatewayConfig config = gatewayService.activate(id);
        auditService.log(actorId, AuditAction.GATEWAY_SET_ACTIVE, "PaymentGatewayConfig",
                config.getId(), config.getGatewayProvider().name(), null, null, "ACTIVE", true);
        return ResponseEntity.ok(ApiResponse.success("Gateway activated", mapper.toGatewayConfigDTO(config)));
    }

    @PostMapping("/{id}/set-default")
    @Operation(summary = "Set a gateway as the default for all new payments")
    public ResponseEntity<ApiResponse<PaymentGatewayConfigDTO>> setDefault(@PathVariable String id) {
        String actorId = currentUserService.getCurrentUserId();
        PaymentGatewayConfig config = gatewayService.setDefault(id);
        auditService.log(actorId, AuditAction.GATEWAY_CONFIG_UPDATED, "PaymentGatewayConfig",
                config.getId(), config.getGatewayProvider().name(), null, null, "SET_DEFAULT", true);
        return ResponseEntity.ok(ApiResponse.success("Default gateway updated", mapper.toGatewayConfigDTO(config)));
    }
}
