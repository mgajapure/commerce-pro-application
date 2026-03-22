package com.commerce_pro_backend.payment.repository;

import com.commerce_pro_backend.payment.entity.PaymentGatewayConfig;
import com.commerce_pro_backend.payment.enums.GatewayProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentGatewayConfigRepository extends JpaRepository<PaymentGatewayConfig, String> {

    Optional<PaymentGatewayConfig> findByGatewayProvider(GatewayProvider provider);

    Optional<PaymentGatewayConfig> findByIsDefaultTrueAndIsActiveTrue();

    List<PaymentGatewayConfig> findByIsActiveTrue();
}
