package com.commerce_pro_backend.payment.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.order.entity.Order;
import com.commerce_pro_backend.order.enums.PaymentStatus;
import com.commerce_pro_backend.order.repository.OrderRepository;
import com.commerce_pro_backend.payment.dto.request.CreatePaymentRequest;
import com.commerce_pro_backend.payment.dto.request.FlagTransactionRequest;
import com.commerce_pro_backend.payment.dto.response.PaymentStatsDTO;
import com.commerce_pro_backend.payment.dto.response.PaymentTransactionDTO;
import com.commerce_pro_backend.payment.dto.response.PaymentTransactionSummaryDTO;
import com.commerce_pro_backend.payment.entity.PaymentTransaction;
import com.commerce_pro_backend.payment.enums.GatewayProvider;
import com.commerce_pro_backend.payment.enums.TransactionStatus;
import com.commerce_pro_backend.payment.enums.TransactionType;
import com.commerce_pro_backend.payment.gateway.GatewayChargeRequest;
import com.commerce_pro_backend.payment.gateway.GatewayChargeResponse;
import com.commerce_pro_backend.payment.gateway.PaymentGatewayPort;
import com.commerce_pro_backend.payment.mapper.PaymentMapper;
import com.commerce_pro_backend.payment.repository.PaymentTransactionRepository;
import com.commerce_pro_backend.payment.specification.PaymentTransactionSpecification;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.service.AuditService;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentTransactionService {

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentNumberService numberService;
    private final PaymentGatewayService gatewayService;
    private final PaymentMapper mapper;
    private final OrderRepository orderRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    // ── READ ──────────────────────────────────────────────────────────────────

    public PageResponse<PaymentTransactionSummaryDTO> list(
            String search, TransactionStatus status, TransactionType type,
            GatewayProvider gateway, String orderId, String customerId,
            Boolean isFlagged, LocalDateTime from, LocalDateTime to, Pageable pageable) {

        Specification<PaymentTransaction> spec = PaymentTransactionSpecification
                .withFilters(search, status, type, gateway, orderId, customerId, isFlagged, from, to);
        Page<PaymentTransaction> page = transactionRepository.findAll(spec, pageable);
        return PageResponse.from(page, mapper.toSummaryDTOList(page.getContent()));
    }

    public PaymentTransactionDTO getById(String id) {
        return mapper.toDTO(findById(id));
    }

    public PaymentTransactionDTO getByRef(String ref) {
        PaymentTransaction t = transactionRepository.findByTransactionRef(ref)
                .orElseThrow(() -> ApiException.notFound("PaymentTransaction", ref));
        return mapper.toDTO(t);
    }

    public PageResponse<PaymentTransactionSummaryDTO> getByOrderId(String orderId, Pageable pageable) {
        Page<PaymentTransaction> page = transactionRepository.findByOrderId(orderId, pageable);
        return PageResponse.from(page, mapper.toSummaryDTOList(page.getContent()));
    }

    public List<PaymentTransactionSummaryDTO> listPending(Pageable pageable) {
        Page<PaymentTransaction> page = transactionRepository.findByStatus(TransactionStatus.AUTHORIZED, pageable);
        return mapper.toSummaryDTOList(page.getContent());
    }

    public List<PaymentTransactionSummaryDTO> listExpiring() {
        LocalDateTime cutoff = LocalDateTime.now().plusHours(24);
        return mapper.toSummaryDTOList(
                transactionRepository.findExpiringAuthorizations(LocalDateTime.now(), cutoff));
    }

    public PaymentStatsDTO getStats() {
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        long activeLinks = 0L; // injected by PaymentLinkService if needed, defaulted here
        return PaymentStatsDTO.builder()
                .totalTransactions(transactionRepository.count())
                .transactionsToday(transactionRepository.countTransactionsSince(startOfToday))
                .totalRevenue(nullSafe(transactionRepository.sumTotalRevenue()))
                .revenueToday(nullSafe(transactionRepository.sumRevenueSince(startOfToday)))
                .pendingCaptureAmount(nullSafe(transactionRepository.sumPendingCaptureAmount()))
                .build();
    }

    // ── WRITE ─────────────────────────────────────────────────────────────────

    @Transactional
    public PaymentTransactionDTO createPayment(CreatePaymentRequest req) {
        String actorId = currentUserService.getCurrentUserId();
        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> ApiException.notFound("Order", req.getOrderId()));

        PaymentGatewayPort gateway = gatewayService.resolveGateway(req.getGatewayProvider());

        GatewayChargeRequest chargeReq = GatewayChargeRequest.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .amount(req.getAmount())
                .currency(req.getCurrency())
                .provider(req.getGatewayProvider())
                .methodType(req.getPaymentMethodType())
                .customerEmail(order.getCustomerEmail())
                .build();

        GatewayChargeResponse gatewayResp = req.getTransactionType() == TransactionType.AUTHORIZE
                ? gateway.authorize(chargeReq) : gateway.charge(chargeReq);

        TransactionStatus resultStatus = gatewayResp.isSuccess()
                ? (req.getTransactionType() == TransactionType.AUTHORIZE
                        ? TransactionStatus.AUTHORIZED : TransactionStatus.CAPTURED)
                : TransactionStatus.FAILED;

        PaymentTransaction txn = PaymentTransaction.builder()
                .transactionRef(numberService.generatePaymentRef())
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .transactionType(req.getTransactionType())
                .status(resultStatus)
                .gatewayProvider(req.getGatewayProvider())
                .gatewayTransactionId(gatewayResp.getGatewayTransactionId())
                .gatewayChargeId(gatewayResp.getGatewayChargeId())
                .authorizationCode(gatewayResp.getAuthorizationCode())
                .gatewayResponseCode(gatewayResp.getResponseCode())
                .gatewayResponseMessage(gatewayResp.getResponseMessage())
                .amount(req.getAmount())
                .currency(req.getCurrency())
                .gatewayFee(gatewayResp.getGatewayFee() != null ? gatewayResp.getGatewayFee() : BigDecimal.ZERO)
                .paymentMethodType(req.getPaymentMethodType())
                .cardLast4(gatewayResp.getCardLast4())
                .cardBrand(gatewayResp.getCardBrand())
                .cardExpMonth(gatewayResp.getCardExpMonth())
                .cardExpYear(gatewayResp.getCardExpYear())
                .expiresAt(req.getTransactionType() == TransactionType.AUTHORIZE
                        ? LocalDateTime.now().plusDays(7) : null)
                .capturedAt(resultStatus == TransactionStatus.CAPTURED ? LocalDateTime.now() : null)
                .failureReason(gatewayResp.getFailureReason())
                .internalNotes(req.getInternalNotes())
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();
        txn.computeNetAmount();
        txn = transactionRepository.save(txn);

        // Update order payment status
        updateOrderPaymentStatus(order, resultStatus, actorId);

        auditService.log(actorId, AuditAction.PAYMENT_CREATED, "PaymentTransaction",
                txn.getId(), txn.getTransactionRef(), null, null, txn.getStatus().name(), gatewayResp.isSuccess());

        return mapper.toDTO(txn);
    }

    @Transactional
    public PaymentTransactionDTO capture(String id, BigDecimal captureAmount) {
        String actorId = currentUserService.getCurrentUserId();
        PaymentTransaction txn = findById(id);

        if (!txn.isCaptureable()) {
            throw ApiException.badRequest("Transaction " + txn.getTransactionRef() + " cannot be captured in status: " + txn.getStatus());
        }

        BigDecimal amount = captureAmount != null ? captureAmount : txn.getAmount();
        PaymentGatewayPort gateway = gatewayService.resolveGateway(txn.getGatewayProvider());
        GatewayChargeResponse resp = gateway.capture(txn.getGatewayTransactionId(), amount);

        txn.setStatus(resp.isSuccess() ? TransactionStatus.CAPTURED : TransactionStatus.FAILED);
        txn.setCapturedAt(resp.isSuccess() ? LocalDateTime.now() : null);
        txn.setUpdatedBy(actorId);
        txn = transactionRepository.save(txn);

        if (resp.isSuccess()) {
            Order order = orderRepository.findById(txn.getOrderId()).orElse(null);
            if (order != null) updateOrderPaymentStatus(order, TransactionStatus.CAPTURED, actorId);
        }

        auditService.log(actorId, AuditAction.PAYMENT_CAPTURED, "PaymentTransaction",
                txn.getId(), txn.getTransactionRef(), null, null, txn.getStatus().name(), resp.isSuccess());
        return mapper.toDTO(txn);
    }

    @Transactional
    public PaymentTransactionDTO voidTransaction(String id) {
        String actorId = currentUserService.getCurrentUserId();
        PaymentTransaction txn = findById(id);

        if (!txn.isVoidable()) {
            throw ApiException.badRequest("Transaction " + txn.getTransactionRef() + " cannot be voided in status: " + txn.getStatus());
        }

        PaymentGatewayPort gateway = gatewayService.resolveGateway(txn.getGatewayProvider());
        GatewayChargeResponse resp = gateway.voidAuthorization(txn.getGatewayTransactionId());

        txn.setStatus(resp.isSuccess() ? TransactionStatus.VOIDED : TransactionStatus.FAILED);
        txn.setVoidedAt(resp.isSuccess() ? LocalDateTime.now() : null);
        txn.setUpdatedBy(actorId);
        txn = transactionRepository.save(txn);

        auditService.log(actorId, AuditAction.PAYMENT_VOIDED, "PaymentTransaction",
                txn.getId(), txn.getTransactionRef(), null, null, txn.getStatus().name(), resp.isSuccess());
        return mapper.toDTO(txn);
    }

    @Transactional
    public PaymentTransactionDTO flagTransaction(String id, FlagTransactionRequest req) {
        String actorId = currentUserService.getCurrentUserId();
        PaymentTransaction txn = findById(id);
        String oldFlag = txn.getIsFlagged().toString();
        txn.setIsFlagged(req.isFlagged());
        txn.setFlagReason(req.isFlagged() ? req.getReason() : null);
        txn.setUpdatedBy(actorId);
        txn = transactionRepository.save(txn);

        auditService.log(actorId, AuditAction.PAYMENT_FLAGGED, "PaymentTransaction",
                txn.getId(), txn.getTransactionRef(), null, oldFlag, String.valueOf(req.isFlagged()), true);
        return mapper.toDTO(txn);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public PaymentTransaction findById(String id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("PaymentTransaction", id));
    }

    private void updateOrderPaymentStatus(Order order, TransactionStatus txnStatus, String actorId) {
        PaymentStatus newStatus = switch (txnStatus) {
            case AUTHORIZED -> PaymentStatus.AUTHORIZED;
            case CAPTURED   -> PaymentStatus.CAPTURED;
            case SETTLED    -> PaymentStatus.CAPTURED;
            case VOIDED     -> PaymentStatus.VOIDED;
            case FAILED     -> PaymentStatus.FAILED;
            default         -> order.getPaymentStatus();
        };
        order.setPaymentStatus(newStatus);
        order.setUpdatedBy(actorId);
        orderRepository.save(order);
    }

    private BigDecimal nullSafe(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
