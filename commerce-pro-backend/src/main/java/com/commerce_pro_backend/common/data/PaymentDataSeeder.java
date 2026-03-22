package com.commerce_pro_backend.common.data;

import com.commerce_pro_backend.order.entity.Order;
import com.commerce_pro_backend.order.enums.OrderStatus;
import com.commerce_pro_backend.order.repository.OrderRepository;
import com.commerce_pro_backend.payment.entity.*;
import com.commerce_pro_backend.payment.enums.*;
import com.commerce_pro_backend.payment.repository.*;
import com.commerce_pro_backend.payment.service.PaymentNumberService;
import com.commerce_pro_backend.payment.service.PayoutNumberService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds Payment module data using JSON configs + real Orders/Customers from prior seeders.
 *
 * Runs at @Order(7) — after OrderDataSeeder (5) and FulfillmentDataSeeder (6).
 *
 * JSON sources (src/main/resources/seed/):
 *   gateway-configs.json, payment-scenarios.json, payment-links.json
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class PaymentDataSeeder {

    private final SeedDataLoader              seedDataLoader;
    private final OrderRepository             orderRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentMethodRepository      methodRepository;
    private final PaymentGatewayConfigRepository gatewayConfigRepository;
    private final PaymentLinkRepository        linkRepository;
    private final RefundRequestRepository      refundRepository;
    private final ChargebackDisputeRepository  chargebackRepository;
    private final PayoutRepository             payoutRepository;
    private final ReconciliationReportRepository reconciliationRepository;
    private final PaymentNumberService         paymentNumberService;
    private final PayoutNumberService          payoutNumberService;

    @Bean
    @Profile("dev")
    @org.springframework.core.annotation.Order(7)
    CommandLineRunner seedPaymentData() {
        return args -> {
            if (transactionRepository.count() > 0) {
                log.info("[PaymentSeeder] Payment data already seeded — skipping");
                return;
            }

            log.info("━━━ [Seed] Payment ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            List<Order> orders = orderRepository.findAll();
            if (orders.isEmpty()) {
                log.warn("[PaymentSeeder] No orders found — skipping payment seed");
                return;
            }

            JsonNode scenariosJson = seedDataLoader.loadTree("seed/payment-scenarios.json");
            JsonNode statusMapping = scenariosJson.get("orderStatusMapping");
            JsonNode defaultScenario = scenariosJson.get("defaultScenario");
            BigDecimal feePercent = new BigDecimal(scenariosJson.get("gatewayFeePercent").asText());
            BigDecimal feeFixed = new BigDecimal(scenariosJson.get("gatewayFeeFixed").asText());

            // ── 1. Gateway Configs ──────────────────────────────────────────
            seedGatewayConfigs();

            // ── 2. Payment Transactions ─────────────────────────────────────
            List<PaymentTransaction> settledTxns = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (Order order : orders) {
                String orderStatusName = order.getStatus().name();
                JsonNode scenario = statusMapping.has(orderStatusName)
                        ? statusMapping.get(orderStatusName)
                        : defaultScenario;

                TransactionType txnType = TransactionType.valueOf(scenario.get("txnType").asText());
                TransactionStatus txnStatus = TransactionStatus.valueOf(scenario.get("txnStatus").asText());
                GatewayProvider gateway = GatewayProvider.valueOf(scenario.get("gateway").asText());
                PaymentMethodType pmType = PaymentMethodType.valueOf(scenario.get("methodType").asText());
                String last4 = scenario.has("cardLast4") && !scenario.get("cardLast4").isNull() ? scenario.get("cardLast4").asText() : null;
                String brand = scenario.has("cardBrand") && !scenario.get("cardBrand").isNull() ? scenario.get("cardBrand").asText() : null;

                BigDecimal amount = order.getTotalAmount() != null ? order.getTotalAmount() : new BigDecimal("99.99");
                BigDecimal fee = amount.multiply(feePercent).add(feeFixed).setScale(4, RoundingMode.HALF_UP);

                PaymentTransaction txn = transactionRepository.save(PaymentTransaction.builder()
                        .transactionRef(paymentNumberService.generatePaymentRef())
                        .orderId(order.getId())
                        .orderNumber(order.getOrderNumber())
                        .customerId(order.getCustomerId())
                        .customerName(order.getCustomerName())
                        .customerEmail(order.getCustomerEmail())
                        .transactionType(txnType).status(txnStatus)
                        .gatewayProvider(gateway)
                        .gatewayTransactionId("ch_seed_" + gateway.name().toLowerCase() + "_" + order.getOrderNumber())
                        .amount(amount)
                        .currency(order.getCurrency() != null ? order.getCurrency() : "USD")
                        .gatewayFee(fee).netAmount(amount.subtract(fee))
                        .paymentMethodType(pmType)
                        .cardLast4(last4).cardBrand(brand)
                        .ipAddress("192.168.1." + (10 + (int) (Math.random() * 240)))
                        .createdBy("SYSTEM").updatedBy("SYSTEM")
                        .build());

                // Set lifecycle timestamps
                if (scenario.has("capturedDaysAgo") && !scenario.get("capturedDaysAgo").isNull()) {
                    txn.setCapturedAt(now.minusDays(scenario.get("capturedDaysAgo").asInt()));
                }
                if (scenario.has("settledDaysAgo") && !scenario.get("settledDaysAgo").isNull()) {
                    txn.setSettledAt(now.minusDays(scenario.get("settledDaysAgo").asInt()));
                }
                if (txnStatus == TransactionStatus.VOIDED) {
                    txn.setVoidedAt(now.minusDays(19));
                }
                transactionRepository.save(txn);

                if (txnStatus == TransactionStatus.SETTLED) {
                    settledTxns.add(txn);
                }
            }
            log.info("[PaymentSeeder] Seeded {} payment transactions", orders.size());

            // ── 3. Payment Methods ──────────────────────────────────────────
            seedPaymentMethods(orders, scenariosJson.get("paymentMethods"));

            // ── 4. Payment Links ────────────────────────────────────────────
            seedPaymentLinks(orders, now);

            // ── 5. Refund Requests ──────────────────────────────────────────
            seedRefundRequests(settledTxns, scenariosJson.get("refundScenarios"), now);

            // ── 6. Chargeback Disputes ──────────────────────────────────────
            seedChargebackDispute(settledTxns, scenariosJson.get("chargebackScenario"), now);

            // ── 7. Payout Batch ─────────────────────────────────────────────
            if (!settledTxns.isEmpty()) seedPayout(settledTxns, now);

            // ── 8. Reconciliation Report ────────────────────────────────────
            if (!settledTxns.isEmpty()) seedReconciliation(settledTxns, now);

            log.info("[PaymentSeeder] Payment seed data complete");
        };
    }

    // ── Gateway Configs ─────────────────────────────────────────────────────────

    private void seedGatewayConfigs() {
        if (gatewayConfigRepository.count() > 0) return;

        JsonNode configsJson = seedDataLoader.loadTree("seed/gateway-configs.json");
        for (JsonNode cfg : configsJson) {
            gatewayConfigRepository.save(PaymentGatewayConfig.builder()
                    .gatewayProvider(GatewayProvider.valueOf(cfg.get("gatewayProvider").asText()))
                    .displayName(cfg.get("displayName").asText())
                    .apiKey(cfg.has("apiKey") && !cfg.get("apiKey").isNull() ? cfg.get("apiKey").asText() : null)
                    .apiSecret(cfg.has("apiSecret") && !cfg.get("apiSecret").isNull() ? cfg.get("apiSecret").asText() : null)
                    .isTestMode(cfg.get("isTestMode").asBoolean())
                    .isActive(cfg.get("isActive").asBoolean())
                    .isDefault(cfg.get("isDefault").asBoolean())
                    .supportedCurrencies(cfg.get("supportedCurrencies").asText())
                    .createdBy("SYSTEM").updatedBy("SYSTEM").build());
        }
        log.info("[PaymentSeeder] Seeded {} gateway configurations", configsJson.size());
    }

    // ── Payment Methods ─────────────────────────────────────────────────────────

    private void seedPaymentMethods(List<Order> orders, JsonNode methodsJson) {
        if (methodRepository.count() > 0) return;

        List<String> seenCustomers = new ArrayList<>();
        int idx = 0;

        for (Order order : orders) {
            if (order.getCustomerId() == null || seenCustomers.contains(order.getCustomerId())) continue;
            if (idx >= methodsJson.size()) break;
            seenCustomers.add(order.getCustomerId());

            JsonNode m = methodsJson.get(idx);
            PaymentMethodType type = PaymentMethodType.valueOf(m.get("methodType").asText());
            GatewayProvider gw = GatewayProvider.valueOf(m.get("gateway").asText());

            PaymentMethod.PaymentMethodBuilder builder = PaymentMethod.builder()
                    .customerId(order.getCustomerId())
                    .methodType(type)
                    .gatewayProvider(gw)
                    .isDefault(idx == 0)
                    .isActive(true)
                    .createdBy("SYSTEM");

            if (type == PaymentMethodType.PAYPAL) {
                builder.accountLabel("PayPal — " + order.getCustomerEmail())
                        .gatewayToken("tok_paypal_seed_" + (idx + 1));
            } else {
                builder.cardLast4(m.get("cardLast4").asText())
                        .cardBrand(m.get("cardBrand").asText())
                        .cardExpMonth(m.get("cardExpMonth").asText())
                        .cardExpYear(m.get("cardExpYear").asText())
                        .cardHolderName(order.getCustomerName())
                        .gatewayToken("tok_stripe_seed_" + (idx + 1));
            }

            methodRepository.save(builder.build());
            idx++;
        }
        log.info("[PaymentSeeder] Seeded {} payment methods", idx);
    }

    // ── Payment Links ───────────────────────────────────────────────────────────

    private void seedPaymentLinks(List<Order> orders, LocalDateTime now) {
        if (linkRepository.count() > 0) return;

        // Active link for a pending order
        orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DRAFT || o.getStatus() == OrderStatus.PENDING_PAYMENT)
                .findFirst()
                .ifPresent(o -> linkRepository.save(PaymentLink.builder()
                        .slug("pay-" + o.getOrderNumber().toLowerCase())
                        .title("Payment for Order " + o.getOrderNumber())
                        .orderId(o.getId()).orderNumber(o.getOrderNumber())
                        .customerId(o.getCustomerId()).customerEmail(o.getCustomerEmail())
                        .status(PaymentLinkStatus.ACTIVE)
                        .amount(o.getTotalAmount()).currency("USD").isOneTime(true)
                        .expiresAt(now.plusDays(7))
                        .description("Complete your payment for order " + o.getOrderNumber())
                        .createdBy("SYSTEM").updatedBy("SYSTEM").build()));

        // Paid link for a delivered order
        orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .findFirst()
                .ifPresent(o -> linkRepository.save(PaymentLink.builder()
                        .slug("pay-" + o.getOrderNumber().toLowerCase())
                        .title("Payment for Order " + o.getOrderNumber())
                        .orderId(o.getId()).orderNumber(o.getOrderNumber())
                        .customerId(o.getCustomerId()).customerEmail(o.getCustomerEmail())
                        .status(PaymentLinkStatus.PAID)
                        .amount(o.getTotalAmount()).currency("USD").isOneTime(true)
                        .paidAt(now.minusDays(5))
                        .description("Payment completed for order " + o.getOrderNumber())
                        .createdBy("SYSTEM").updatedBy("SYSTEM").build()));

        // Static links from JSON
        JsonNode linksJson = seedDataLoader.loadTree("seed/payment-links.json");
        for (JsonNode link : linksJson) {
            linkRepository.save(PaymentLink.builder()
                    .slug(link.get("slug").asText())
                    .title(link.get("title").asText())
                    .status(PaymentLinkStatus.valueOf(link.get("status").asText()))
                    .amount(new BigDecimal(link.get("amount").asText()))
                    .currency(link.get("currency").asText())
                    .isOneTime(link.get("isOneTime").asBoolean())
                    .maxUses(link.has("maxUses") ? link.get("maxUses").asInt() : null)
                    .usesCount(link.has("usesCount") ? link.get("usesCount").asInt() : 0)
                    .expiresAt(link.has("expiredDaysAgo") ? now.minusDays(link.get("expiredDaysAgo").asInt()) : null)
                    .description(link.get("description").asText())
                    .createdBy("SYSTEM").updatedBy("SYSTEM").build());
        }
        log.info("[PaymentSeeder] Seeded payment links");
    }

    // ── Refund Requests ─────────────────────────────────────────────────────────

    private void seedRefundRequests(List<PaymentTransaction> settledTxns, JsonNode refundScenarios, LocalDateTime now) {
        if (refundRepository.count() > 0 || refundScenarios == null) return;

        int count = 0;
        for (JsonNode rs : refundScenarios) {
            int txnIdx = rs.get("settledTxnIndex").asInt();
            if (txnIdx >= settledTxns.size()) continue;

            PaymentTransaction txn = settledTxns.get(txnIdx);
            BigDecimal refundPct = new BigDecimal(rs.get("refundPercent").asText());
            BigDecimal refundAmt = txn.getAmount().multiply(refundPct).setScale(4, RoundingMode.HALF_UP);
            RefundStatus status = RefundStatus.valueOf(rs.get("status").asText());
            boolean completed = status == RefundStatus.COMPLETED;

            RefundRequest.RefundRequestBuilder builder = RefundRequest.builder()
                    .refundRef(paymentNumberService.generateRefundRef())
                    .paymentTransactionId(txn.getId())
                    .orderId(txn.getOrderId()).orderNumber(txn.getOrderNumber())
                    .customerId(txn.getCustomerId()).customerName(txn.getCustomerName())
                    .status(status)
                    .reason(RefundReason.valueOf(rs.get("reason").asText()))
                    .reasonDetail(rs.get("reasonDetail").asText())
                    .refundAmount(refundAmt).currency("USD")
                    .requestedBy("SYSTEM")
                    .createdBy("SYSTEM").updatedBy("SYSTEM");

            if (completed) {
                builder.reviewedBy("SYSTEM")
                        .reviewedAt(now.minusDays(4))
                        .reviewNotes(rs.get("reviewNotes").asText())
                        .gatewayRefundId("re_seed_stripe_" + String.format("%03d", count + 1))
                        .gatewayStatus("succeeded")
                        .processedAt(now.minusDays(rs.get("processedDaysAgo").asInt()));
            }

            refundRepository.save(builder.build());
            count++;
        }
        log.info("[PaymentSeeder] Seeded {} refund requests", count);
    }

    // ── Chargeback Disputes ─────────────────────────────────────────────────────

    private void seedChargebackDispute(List<PaymentTransaction> settledTxns, JsonNode cbScenario, LocalDateTime now) {
        if (chargebackRepository.count() > 0 || cbScenario == null) return;

        int txnIdx = cbScenario.get("settledTxnIndex").asInt();
        if (txnIdx >= settledTxns.size()) return;

        PaymentTransaction txn = settledTxns.get(txnIdx);
        chargebackRepository.save(ChargebackDispute.builder()
                .disputeRef(paymentNumberService.generateDisputeRef())
                .paymentTransactionId(txn.getId())
                .orderId(txn.getOrderId()).orderNumber(txn.getOrderNumber())
                .customerId(txn.getCustomerId()).customerName(txn.getCustomerName())
                .status(ChargebackStatus.valueOf(cbScenario.get("status").asText()))
                .reason(ChargebackReason.valueOf(cbScenario.get("reason").asText()))
                .reasonCode(cbScenario.get("reasonCode").asText())
                .disputedAmount(txn.getAmount()).currency("USD")
                .gatewayDisputeId("dp_seed_stripe_001")
                .responseDeadline(now.plusDays(cbScenario.get("responseDeadlineDaysFromNow").asInt()))
                .internalNotes(cbScenario.get("internalNotes").asText())
                .createdBy("SYSTEM").updatedBy("SYSTEM").build());

        log.info("[PaymentSeeder] Seeded 1 chargeback dispute");
    }

    // ── Payout Batch ────────────────────────────────────────────────────────────

    private void seedPayout(List<PaymentTransaction> settledTxns, LocalDateTime now) {
        if (payoutRepository.count() > 0) return;

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;
        List<PayoutItem> items = new ArrayList<>();

        Payout payout = Payout.builder()
                .payoutRef(payoutNumberService.generatePayoutRef())
                .status(PayoutStatus.COMPLETED)
                .periodStart(now.minusDays(30)).periodEnd(now.minusDays(1))
                .currency("USD")
                .bankAccountName("Commerce Pro Inc.").bankAccountLast4("6789")
                .initiatedAt(now.minusDays(1)).completedAt(now.minusHours(6))
                .estimatedArrival(now.plusDays(2))
                .notes("Weekly payout batch — all settled transactions")
                .createdBy("SYSTEM").updatedBy("SYSTEM").build();

        for (PaymentTransaction txn : settledTxns) {
            BigDecimal fee = txn.getGatewayFee() != null ? txn.getGatewayFee() : BigDecimal.ZERO;
            BigDecimal net = txn.getAmount().subtract(fee);
            totalGross = totalGross.add(txn.getAmount());
            totalFees = totalFees.add(fee);

            items.add(PayoutItem.builder()
                    .payout(payout)
                    .paymentTransactionId(txn.getId())
                    .transactionRef(txn.getTransactionRef())
                    .orderId(txn.getOrderId()).orderNumber(txn.getOrderNumber())
                    .grossAmount(txn.getAmount()).feeAmount(fee).netAmount(net)
                    .build());
        }

        payout.setTotalAmount(totalGross);
        payout.setTotalFees(totalFees);
        payout.setNetAmount(totalGross.subtract(totalFees));
        payout.setItems(items);
        payoutRepository.save(payout);
        log.info("[PaymentSeeder] Seeded 1 completed payout with {} items", items.size());
    }

    // ── Reconciliation Report ───────────────────────────────────────────────────

    private void seedReconciliation(List<PaymentTransaction> settledTxns, LocalDateTime now) {
        if (reconciliationRepository.count() > 0) return;

        BigDecimal totalRevenue = settledTxns.stream()
                .map(PaymentTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFees = settledTxns.stream()
                .map(t -> t.getGatewayFee() != null ? t.getGatewayFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ReconciliationReport report = ReconciliationReport.builder()
                .reportRef(paymentNumberService.generateReconRef())
                .periodStart(now.minusDays(30)).periodEnd(now.minusDays(1))
                .status(ReconciliationStatus.COMPLETED)
                .totalTransactions((long) settledTxns.size())
                .totalRevenue(totalRevenue)
                .totalRefunds(BigDecimal.ZERO).totalChargebacks(BigDecimal.ZERO)
                .totalFees(totalFees)
                .netRevenue(totalRevenue.subtract(totalFees))
                .varianceAmount(BigDecimal.ZERO)
                .matchedCount((long) settledTxns.size()).unmatchedCount(0L)
                .generatedAt(now.minusHours(12)).generatedBy("SYSTEM")
                .notes("Monthly reconciliation — all transactions matched")
                .build();

        List<ReconciliationEntry> entries = new ArrayList<>();
        for (PaymentTransaction txn : settledTxns) {
            entries.add(ReconciliationEntry.builder()
                    .report(report)
                    .paymentTransactionId(txn.getId())
                    .transactionRef(txn.getTransactionRef())
                    .internalAmount(txn.getAmount()).gatewayAmount(txn.getAmount())
                    .variance(BigDecimal.ZERO).matchStatus("MATCHED")
                    .build());
        }
        report.setEntries(entries);
        reconciliationRepository.save(report);
        log.info("[PaymentSeeder] Seeded 1 reconciliation report with {} entries", entries.size());
    }
}
