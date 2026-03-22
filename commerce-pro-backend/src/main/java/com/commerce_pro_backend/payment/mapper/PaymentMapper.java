package com.commerce_pro_backend.payment.mapper;

import com.commerce_pro_backend.payment.dto.response.*;
import com.commerce_pro_backend.payment.entity.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentMapper {

    @Value("${app.payment.payment-link-base-url:http://localhost:4200/pay}")
    private String payLinkBaseUrl;

    // ── PaymentTransaction ────────────────────────────────────────────────────

    public PaymentTransactionDTO toDTO(PaymentTransaction t) {
        if (t == null) return null;
        return PaymentTransactionDTO.builder()
                .id(t.getId())
                .transactionRef(t.getTransactionRef())
                .orderId(t.getOrderId())
                .orderNumber(t.getOrderNumber())
                .customerId(t.getCustomerId())
                .customerName(t.getCustomerName())
                .customerEmail(t.getCustomerEmail())
                .transactionType(t.getTransactionType())
                .status(t.getStatus())
                .gatewayProvider(t.getGatewayProvider())
                .gatewayTransactionId(t.getGatewayTransactionId())
                .gatewayChargeId(t.getGatewayChargeId())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .gatewayFee(t.getGatewayFee())
                .netAmount(t.getNetAmount())
                .paymentMethodType(t.getPaymentMethodType())
                .cardLast4(t.getCardLast4())
                .cardBrand(t.getCardBrand())
                .cardExpMonth(t.getCardExpMonth())
                .cardExpYear(t.getCardExpYear())
                .authorizationCode(t.getAuthorizationCode())
                .expiresAt(t.getExpiresAt())
                .capturedAt(t.getCapturedAt())
                .settledAt(t.getSettledAt())
                .riskScore(t.getRiskScore())
                .isFlagged(t.getIsFlagged())
                .flagReason(t.getFlagReason())
                .failureReason(t.getFailureReason())
                .internalNotes(t.getInternalNotes())
                .parentTransactionId(t.getParentTransactionId())
                .paymentLinkId(t.getPaymentLinkId())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .createdBy(t.getCreatedBy())
                .build();
    }

    public PaymentTransactionSummaryDTO toSummaryDTO(PaymentTransaction t) {
        if (t == null) return null;
        return PaymentTransactionSummaryDTO.builder()
                .id(t.getId())
                .transactionRef(t.getTransactionRef())
                .orderNumber(t.getOrderNumber())
                .customerName(t.getCustomerName())
                .transactionType(t.getTransactionType())
                .status(t.getStatus())
                .gatewayProvider(t.getGatewayProvider())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .cardLast4(t.getCardLast4())
                .cardBrand(t.getCardBrand())
                .isFlagged(t.getIsFlagged())
                .createdAt(t.getCreatedAt())
                .build();
    }

    public List<PaymentTransactionSummaryDTO> toSummaryDTOList(List<PaymentTransaction> list) {
        return list.stream().map(this::toSummaryDTO).toList();
    }

    // ── RefundRequest ─────────────────────────────────────────────────────────

    public RefundRequestDTO toRefundDTO(RefundRequest r) {
        if (r == null) return null;
        return RefundRequestDTO.builder()
                .id(r.getId())
                .refundRef(r.getRefundRef())
                .paymentTransactionId(r.getPaymentTransactionId())
                .orderId(r.getOrderId())
                .orderNumber(r.getOrderNumber())
                .customerId(r.getCustomerId())
                .customerName(r.getCustomerName())
                .status(r.getStatus())
                .reason(r.getReason())
                .reasonDetail(r.getReasonDetail())
                .refundAmount(r.getRefundAmount())
                .currency(r.getCurrency())
                .refundShipping(r.getRefundShipping())
                .requestedBy(r.getRequestedBy())
                .reviewedBy(r.getReviewedBy())
                .reviewedAt(r.getReviewedAt())
                .reviewNotes(r.getReviewNotes())
                .gatewayRefundId(r.getGatewayRefundId())
                .processedAt(r.getProcessedAt())
                .failureReason(r.getFailureReason())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    public RefundRequestSummaryDTO toRefundSummary(RefundRequest r) {
        if (r == null) return null;
        return RefundRequestSummaryDTO.builder()
                .id(r.getId())
                .refundRef(r.getRefundRef())
                .orderNumber(r.getOrderNumber())
                .customerName(r.getCustomerName())
                .status(r.getStatus())
                .reason(r.getReason())
                .refundAmount(r.getRefundAmount())
                .currency(r.getCurrency())
                .createdAt(r.getCreatedAt())
                .build();
    }

    public List<RefundRequestSummaryDTO> toRefundSummaryList(List<RefundRequest> list) {
        return list.stream().map(this::toRefundSummary).toList();
    }

    // ── ChargebackDispute ─────────────────────────────────────────────────────

    public ChargebackDisputeDTO toChargebackDTO(ChargebackDispute c) {
        if (c == null) return null;
        return ChargebackDisputeDTO.builder()
                .id(c.getId())
                .disputeRef(c.getDisputeRef())
                .paymentTransactionId(c.getPaymentTransactionId())
                .orderId(c.getOrderId())
                .orderNumber(c.getOrderNumber())
                .customerId(c.getCustomerId())
                .customerName(c.getCustomerName())
                .status(c.getStatus())
                .reason(c.getReason())
                .reasonCode(c.getReasonCode())
                .disputedAmount(c.getDisputedAmount())
                .currency(c.getCurrency())
                .gatewayDisputeId(c.getGatewayDisputeId())
                .responseDeadline(c.getResponseDeadline())
                .evidenceSubmittedAt(c.getEvidenceSubmittedAt())
                .evidenceNotes(c.getEvidenceNotes())
                .resolvedAt(c.getResolvedAt())
                .resolutionNotes(c.getResolutionNotes())
                .isWon(c.getIsWon())
                .deadlineApproaching(c.isDeadlineApproaching())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    public ChargebackSummaryDTO toChargebackSummary(ChargebackDispute c) {
        if (c == null) return null;
        return ChargebackSummaryDTO.builder()
                .id(c.getId())
                .disputeRef(c.getDisputeRef())
                .orderNumber(c.getOrderNumber())
                .customerName(c.getCustomerName())
                .status(c.getStatus())
                .reason(c.getReason())
                .disputedAmount(c.getDisputedAmount())
                .responseDeadline(c.getResponseDeadline())
                .deadlineApproaching(c.isDeadlineApproaching())
                .createdAt(c.getCreatedAt())
                .build();
    }

    public List<ChargebackSummaryDTO> toChargebackSummaryList(List<ChargebackDispute> list) {
        return list.stream().map(this::toChargebackSummary).toList();
    }

    // ── ReconciliationReport ──────────────────────────────────────────────────

    public ReconciliationReportDTO toReconDTO(ReconciliationReport r) {
        if (r == null) return null;
        return ReconciliationReportDTO.builder()
                .id(r.getId())
                .reportRef(r.getReportRef())
                .periodStart(r.getPeriodStart())
                .periodEnd(r.getPeriodEnd())
                .status(r.getStatus())
                .totalTransactions(r.getTotalTransactions())
                .totalRevenue(r.getTotalRevenue())
                .totalRefunds(r.getTotalRefunds())
                .totalChargebacks(r.getTotalChargebacks())
                .totalFees(r.getTotalFees())
                .netRevenue(r.getNetRevenue())
                .varianceAmount(r.getVarianceAmount())
                .matchedCount(r.getMatchedCount())
                .unmatchedCount(r.getUnmatchedCount())
                .generatedAt(r.getGeneratedAt())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .build();
    }

    public ReconciliationSummaryDTO toReconSummary(ReconciliationReport r) {
        if (r == null) return null;
        return ReconciliationSummaryDTO.builder()
                .id(r.getId())
                .reportRef(r.getReportRef())
                .periodStart(r.getPeriodStart())
                .periodEnd(r.getPeriodEnd())
                .status(r.getStatus())
                .totalTransactions(r.getTotalTransactions())
                .netRevenue(r.getNetRevenue())
                .unmatchedCount(r.getUnmatchedCount())
                .generatedAt(r.getGeneratedAt())
                .build();
    }

    public List<ReconciliationSummaryDTO> toReconSummaryList(List<ReconciliationReport> list) {
        return list.stream().map(this::toReconSummary).toList();
    }

    // ── Payout ────────────────────────────────────────────────────────────────

    public PayoutDTO toPayoutDTO(Payout p) {
        if (p == null) return null;
        return PayoutDTO.builder()
                .id(p.getId())
                .payoutRef(p.getPayoutRef())
                .status(p.getStatus())
                .periodStart(p.getPeriodStart())
                .periodEnd(p.getPeriodEnd())
                .totalAmount(p.getTotalAmount())
                .totalFees(p.getTotalFees())
                .netAmount(p.getNetAmount())
                .currency(p.getCurrency())
                .bankAccountName(p.getBankAccountName())
                .bankAccountLast4(p.getBankAccountLast4())
                .initiatedAt(p.getInitiatedAt())
                .estimatedArrival(p.getEstimatedArrival())
                .completedAt(p.getCompletedAt())
                .failureReason(p.getFailureReason())
                .notes(p.getNotes())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    public PayoutSummaryDTO toPayoutSummary(Payout p) {
        if (p == null) return null;
        return PayoutSummaryDTO.builder()
                .id(p.getId())
                .payoutRef(p.getPayoutRef())
                .status(p.getStatus())
                .netAmount(p.getNetAmount())
                .currency(p.getCurrency())
                .periodStart(p.getPeriodStart())
                .periodEnd(p.getPeriodEnd())
                .createdAt(p.getCreatedAt())
                .build();
    }

    public List<PayoutSummaryDTO> toPayoutSummaryList(List<Payout> list) {
        return list.stream().map(this::toPayoutSummary).toList();
    }

    // ── PaymentLink ───────────────────────────────────────────────────────────

    public PaymentLinkDTO toLinkDTO(PaymentLink l) {
        if (l == null) return null;
        return PaymentLinkDTO.builder()
                .id(l.getId())
                .slug(l.getSlug())
                .payUrl(payLinkBaseUrl + "/" + l.getSlug())
                .title(l.getTitle())
                .orderId(l.getOrderId())
                .orderNumber(l.getOrderNumber())
                .customerEmail(l.getCustomerEmail())
                .status(l.getStatus())
                .amount(l.getAmount())
                .currency(l.getCurrency())
                .isOneTime(l.getIsOneTime())
                .maxUses(l.getMaxUses())
                .usesCount(l.getUsesCount())
                .expiresAt(l.getExpiresAt())
                .description(l.getDescription())
                .paidAt(l.getPaidAt())
                .payable(l.isPayable())
                .createdAt(l.getCreatedAt())
                .build();
    }

    public PaymentLinkSummaryDTO toLinkSummary(PaymentLink l) {
        if (l == null) return null;
        return PaymentLinkSummaryDTO.builder()
                .id(l.getId())
                .slug(l.getSlug())
                .title(l.getTitle())
                .status(l.getStatus())
                .amount(l.getAmount())
                .currency(l.getCurrency())
                .usesCount(l.getUsesCount())
                .expiresAt(l.getExpiresAt())
                .createdAt(l.getCreatedAt())
                .build();
    }

    public List<PaymentLinkSummaryDTO> toLinkSummaryList(List<PaymentLink> list) {
        return list.stream().map(this::toLinkSummary).toList();
    }

    // ── PaymentMethod ─────────────────────────────────────────────────────────

    public PaymentMethodDTO toMethodDTO(PaymentMethod m) {
        if (m == null) return null;
        return PaymentMethodDTO.builder()
                .id(m.getId())
                .customerId(m.getCustomerId())
                .methodType(m.getMethodType())
                .gatewayProvider(m.getGatewayProvider())
                .cardLast4(m.getCardLast4())
                .cardBrand(m.getCardBrand())
                .cardExpMonth(m.getCardExpMonth())
                .cardExpYear(m.getCardExpYear())
                .cardHolderName(m.getCardHolderName())
                .accountLabel(m.getAccountLabel())
                .isDefault(m.getIsDefault())
                .isActive(m.getIsActive())
                .lastUsedAt(m.getLastUsedAt())
                .createdAt(m.getCreatedAt())
                .build();
    }

    public List<PaymentMethodDTO> toMethodDTOList(List<PaymentMethod> list) {
        return list.stream().map(this::toMethodDTO).toList();
    }

    // ── PaymentGatewayConfig ──────────────────────────────────────────────────

    public PaymentGatewayConfigDTO toGatewayConfigDTO(PaymentGatewayConfig g) {
        if (g == null) return null;
        // NOTE: apiKey, apiSecret, webhookSecret are intentionally excluded from response
        return PaymentGatewayConfigDTO.builder()
                .id(g.getId())
                .gatewayProvider(g.getGatewayProvider())
                .displayName(g.getDisplayName())
                .isTestMode(g.getIsTestMode())
                .isActive(g.getIsActive())
                .isDefault(g.getIsDefault())
                .supportedCurrencies(g.getSupportedCurrencies())
                .webhookUrl(g.getWebhookUrl())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }

    public List<PaymentGatewayConfigDTO> toGatewayConfigDTOList(List<PaymentGatewayConfig> list) {
        return list.stream().map(this::toGatewayConfigDTO).toList();
    }
}
