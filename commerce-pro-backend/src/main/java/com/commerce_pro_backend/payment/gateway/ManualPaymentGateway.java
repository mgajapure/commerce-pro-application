package com.commerce_pro_backend.payment.gateway;

import com.commerce_pro_backend.payment.enums.GatewayProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Development stub for the payment gateway.
 * Always succeeds — simulates a 2.9% + $0.30 fee (Stripe-like).
 * Swap this for a real implementation (StripeGateway, PayPalGateway, etc.)
 * by implementing PaymentGatewayPort with that provider's SDK.
 */
@Slf4j
@Component
public class ManualPaymentGateway implements PaymentGatewayPort {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.029");
    private static final BigDecimal FEE_FIXED = new BigDecimal("0.30");

    @Override
    public GatewayChargeResponse charge(GatewayChargeRequest request) {
        log.info("[ManualGateway] Charging {} {} for order {}", request.getCurrency(), request.getAmount(), request.getOrderId());
        BigDecimal fee = request.getAmount().multiply(FEE_RATE).add(FEE_FIXED).setScale(4, RoundingMode.HALF_UP);
        return GatewayChargeResponse.builder()
                .success(true)
                .gatewayTransactionId("ch_manual_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                .gatewayChargeId("ch_" + System.currentTimeMillis())
                .authorizationCode("AUTH" + (int)(Math.random() * 900000 + 100000))
                .gatewayFee(fee)
                .responseCode("200")
                .responseMessage("Manual charge processed successfully")
                .cardLast4(request.getCardNumber() != null && request.getCardNumber().length() >= 4
                        ? request.getCardNumber().substring(request.getCardNumber().length() - 4) : "4242")
                .cardBrand("VISA")
                .cardExpMonth(request.getCardExpMonth() != null ? request.getCardExpMonth() : "12")
                .cardExpYear(request.getCardExpYear() != null ? request.getCardExpYear() : "2027")
                .build();
    }

    @Override
    public GatewayChargeResponse authorize(GatewayChargeRequest request) {
        log.info("[ManualGateway] Authorizing {} {} for order {}", request.getCurrency(), request.getAmount(), request.getOrderId());
        BigDecimal fee = request.getAmount().multiply(FEE_RATE).add(FEE_FIXED).setScale(4, RoundingMode.HALF_UP);
        return GatewayChargeResponse.builder()
                .success(true)
                .gatewayTransactionId("auth_manual_" + UUID.randomUUID().toString().replace("-", "").substring(0, 22))
                .gatewayChargeId("auth_" + System.currentTimeMillis())
                .authorizationCode("AUTH" + (int)(Math.random() * 900000 + 100000))
                .gatewayFee(fee)
                .responseCode("200")
                .responseMessage("Manual authorization placed successfully")
                .cardLast4("4242")
                .cardBrand("VISA")
                .cardExpMonth("12")
                .cardExpYear("2027")
                .build();
    }

    @Override
    public GatewayChargeResponse capture(String gatewayTransactionId, BigDecimal amount) {
        log.info("[ManualGateway] Capturing {} for txn {}", amount, gatewayTransactionId);
        return GatewayChargeResponse.builder()
                .success(true)
                .gatewayTransactionId(gatewayTransactionId)
                .gatewayChargeId("cap_" + System.currentTimeMillis())
                .responseCode("200")
                .responseMessage("Manual capture processed successfully")
                .build();
    }

    @Override
    public GatewayChargeResponse voidAuthorization(String gatewayTransactionId) {
        log.info("[ManualGateway] Voiding txn {}", gatewayTransactionId);
        return GatewayChargeResponse.builder()
                .success(true)
                .gatewayTransactionId(gatewayTransactionId)
                .responseCode("200")
                .responseMessage("Manual void processed successfully")
                .build();
    }

    @Override
    public GatewayRefundResponse refund(String gatewayTransactionId, BigDecimal amount, String reason) {
        log.info("[ManualGateway] Refunding {} for txn {}", amount, gatewayTransactionId);
        return GatewayRefundResponse.builder()
                .success(true)
                .gatewayRefundId("re_manual_" + UUID.randomUUID().toString().replace("-", "").substring(0, 22))
                .gatewayStatus("succeeded")
                .responseCode("200")
                .responseMessage("Manual refund processed successfully")
                .build();
    }

    @Override
    public boolean supports(GatewayProvider provider) {
        return provider == GatewayProvider.MANUAL;
    }
}
