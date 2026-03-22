package com.commerce_pro_backend.payment.service;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.payment.entity.PaymentGatewayConfig;
import com.commerce_pro_backend.payment.enums.GatewayProvider;
import com.commerce_pro_backend.payment.gateway.PaymentGatewayPort;
import com.commerce_pro_backend.payment.repository.PaymentGatewayConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Resolves the correct PaymentGatewayPort implementation at runtime.
 * Also manages CRUD for PaymentGatewayConfig entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentGatewayService {

    private final List<PaymentGatewayPort> gateways;   // Spring injects all implementations
    private final PaymentGatewayConfigRepository gatewayConfigRepository;

    /** Returns the gateway implementation that handles the given provider. */
    public PaymentGatewayPort resolveGateway(GatewayProvider provider) {
        return gateways.stream()
                .filter(g -> g.supports(provider))
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest(
                        "No gateway implementation registered for provider: " + provider));
    }

    /** Returns the default active gateway implementation. */
    public PaymentGatewayPort resolveDefaultGateway() {
        PaymentGatewayConfig config = gatewayConfigRepository.findByIsDefaultTrueAndIsActiveTrue()
                .orElseThrow(() -> ApiException.badRequest("No default active payment gateway configured"));
        return resolveGateway(config.getGatewayProvider());
    }

    public List<PaymentGatewayConfig> listAll() {
        return gatewayConfigRepository.findAll();
    }

    public PaymentGatewayConfig getById(String id) {
        return gatewayConfigRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("PaymentGatewayConfig", id));
    }

    @Transactional
    public PaymentGatewayConfig save(PaymentGatewayConfig config) {
        return gatewayConfigRepository.save(config);
    }

    @Transactional
    public PaymentGatewayConfig setDefault(String id) {
        // Clear existing default
        gatewayConfigRepository.findByIsDefaultTrueAndIsActiveTrue()
                .ifPresent(c -> { c.setIsDefault(false); gatewayConfigRepository.save(c); });
        PaymentGatewayConfig config = getById(id);
        config.setIsDefault(true);
        config.setIsActive(true);
        return gatewayConfigRepository.save(config);
    }

    @Transactional
    public PaymentGatewayConfig activate(String id) {
        PaymentGatewayConfig config = getById(id);
        config.setIsActive(true);
        return gatewayConfigRepository.save(config);
    }
}
