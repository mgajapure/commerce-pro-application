package com.commerce_pro_backend.payment.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.order.entity.Order;
import com.commerce_pro_backend.order.enums.PaymentStatus;
import com.commerce_pro_backend.order.repository.OrderRepository;
import com.commerce_pro_backend.payment.dto.request.CreateRefundRequest;
import com.commerce_pro_backend.payment.dto.request.ReviewRefundRequest;
import com.commerce_pro_backend.payment.dto.response.RefundRequestDTO;
import com.commerce_pro_backend.payment.dto.response.RefundRequestSummaryDTO;
import com.commerce_pro_backend.payment.entity.PaymentTransaction;
import com.commerce_pro_backend.payment.entity.RefundRequest;
import com.commerce_pro_backend.payment.enums.RefundStatus;
import com.commerce_pro_backend.payment.gateway.GatewayRefundResponse;
import com.commerce_pro_backend.payment.gateway.PaymentGatewayPort;
import com.commerce_pro_backend.payment.mapper.PaymentMapper;
import com.commerce_pro_backend.payment.repository.PaymentTransactionRepository;
import com.commerce_pro_backend.payment.repository.RefundRequestRepository;
import com.commerce_pro_backend.payment.specification.RefundRequestSpecification;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.service.AuditService;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {

    private final RefundRequestRepository refundRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentNumberService numberService;
    private final PaymentGatewayService gatewayService;
    private final PaymentMapper mapper;
    private final OrderRepository orderRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    // ── READ ──────────────────────────────────────────────────────────────────

    public PageResponse<RefundRequestSummaryDTO> list(RefundStatus status, String orderId, Pageable pageable) {
        var spec = RefundRequestSpecification.withFilters(status, orderId);
        Page<RefundRequest> page = refundRepository.findAll(spec, pageable);
        return PageResponse.from(page, mapper.toRefundSummaryList(page.getContent()));
    }

    public RefundRequestDTO getById(String id) {
        return mapper.toRefundDTO(findById(id));
    }

    public List<RefundRequestSummaryDTO> getByOrderId(String orderId) {
        return mapper.toRefundSummaryList(refundRepository.findByOrderId(orderId));
    }

    // ── WRITE ─────────────────────────────────────────────────────────────────

    @Transactional
    public RefundRequestDTO requestRefund(CreateRefundRequest req) {
        String actorId = currentUserService.getCurrentUserId();

        PaymentTransaction txn = transactionRepository.findById(req.getPaymentTransactionId())
                .orElseThrow(() -> ApiException.notFound("PaymentTransaction", req.getPaymentTransactionId()));

        if (!txn.isRefundable()) {
            throw ApiException.badRequest("Transaction " + txn.getTransactionRef() + " is not in a refundable state: " + txn.getStatus());
        }

        // Validate refund amount doesn't exceed original
        BigDecimal alreadyRefunded = refundRepository.sumCompletedRefundsForOrder(txn.getOrderId());
        BigDecimal maxRefundable = txn.getAmount().subtract(alreadyRefunded);
        if (req.getRefundAmount().compareTo(maxRefundable) > 0) {
            throw ApiException.badRequest(
                    "Refund amount " + req.getRefundAmount() + " exceeds maximum refundable amount: " + maxRefundable);
        }

        Order order = orderRepository.findById(txn.getOrderId())
                .orElseThrow(() -> ApiException.notFound("Order", txn.getOrderId()));

        RefundRequest refund = RefundRequest.builder()
                .refundRef(numberService.generateRefundRef())
                .paymentTransactionId(txn.getId())
                .orderId(txn.getOrderId())
                .orderNumber(txn.getOrderNumber())
                .customerId(txn.getCustomerId())
                .customerName(txn.getCustomerName())
                .status(RefundStatus.PENDING)
                .reason(req.getReason())
                .reasonDetail(req.getReasonDetail())
                .refundAmount(req.getRefundAmount())
                .currency(txn.getCurrency())
                .refundShipping(req.getRefundShipping() != null && req.getRefundShipping())
                .requestedBy(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        refund = refundRepository.save(refund);
        order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        orderRepository.save(order);

        auditService.log(actorId, AuditAction.REFUND_REQUESTED, "RefundRequest",
                refund.getId(), refund.getRefundRef(), null, null, refund.getStatus().name(), true);
        return mapper.toRefundDTO(refund);
    }

    @Transactional
    public RefundRequestDTO approve(String id, ReviewRefundRequest req) {
        String actorId = currentUserService.getCurrentUserId();
        RefundRequest refund = findById(id);
        if (!refund.isApprovable()) {
            throw ApiException.badRequest("Refund " + refund.getRefundRef() + " cannot be approved in status: " + refund.getStatus());
        }
        refund.setStatus(RefundStatus.APPROVED);
        refund.setReviewedBy(actorId);
        refund.setReviewedAt(LocalDateTime.now());
        refund.setReviewNotes(req.getNotes());
        refund.setUpdatedBy(actorId);
        refund = refundRepository.save(refund);
        auditService.log(actorId, AuditAction.REFUND_APPROVED, "RefundRequest",
                refund.getId(), refund.getRefundRef(), null, RefundStatus.PENDING.name(), RefundStatus.APPROVED.name(), true);
        return mapper.toRefundDTO(refund);
    }

    @Transactional
    public RefundRequestDTO reject(String id, ReviewRefundRequest req) {
        String actorId = currentUserService.getCurrentUserId();
        RefundRequest refund = findById(id);
        if (!refund.isRejectable()) {
            throw ApiException.badRequest("Refund " + refund.getRefundRef() + " cannot be rejected in status: " + refund.getStatus());
        }
        refund.setStatus(RefundStatus.REJECTED);
        refund.setReviewedBy(actorId);
        refund.setReviewedAt(LocalDateTime.now());
        refund.setReviewNotes(req.getNotes());
        refund.setUpdatedBy(actorId);
        refund = refundRepository.save(refund);
        auditService.log(actorId, AuditAction.REFUND_REJECTED, "RefundRequest",
                refund.getId(), refund.getRefundRef(), null, null, RefundStatus.REJECTED.name(), true);
        return mapper.toRefundDTO(refund);
    }

    @Transactional
    public RefundRequestDTO process(String id) {
        String actorId = currentUserService.getCurrentUserId();
        RefundRequest refund = findById(id);
        if (!refund.isProcessable()) {
            throw ApiException.badRequest("Refund " + refund.getRefundRef() + " must be APPROVED before processing. Current status: " + refund.getStatus());
        }

        String paymentTxnId = refund.getPaymentTransactionId();
        PaymentTransaction txn = transactionRepository.findById(paymentTxnId)
                .orElseThrow(() -> ApiException.notFound("PaymentTransaction", paymentTxnId));

        refund.setStatus(RefundStatus.PROCESSING);
        refundRepository.save(refund);

        PaymentGatewayPort gateway = gatewayService.resolveGateway(txn.getGatewayProvider());
        GatewayRefundResponse resp = gateway.refund(
                txn.getGatewayTransactionId(), refund.getRefundAmount(), refund.getReason().name());

        if (resp.isSuccess()) {
            refund.setStatus(RefundStatus.COMPLETED);
            refund.setGatewayRefundId(resp.getGatewayRefundId());
            refund.setGatewayStatus(resp.getGatewayStatus());
            refund.setProcessedAt(LocalDateTime.now());

            // Update order refunded amount and payment status
            Order order = orderRepository.findById(refund.getOrderId()).orElse(null);
            if (order != null) {
                BigDecimal newRefunded = order.getRefundedAmount().add(refund.getRefundAmount());
                order.setRefundedAmount(newRefunded);
                order.setPaymentStatus(newRefunded.compareTo(order.getTotalAmount()) >= 0
                        ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
                order.setUpdatedBy(actorId);
                orderRepository.save(order);
            }

            // Mark original transaction
            txn.setStatus(refund.getRefundAmount().compareTo(txn.getAmount()) >= 0
                    ? com.commerce_pro_backend.payment.enums.TransactionStatus.REFUNDED
                    : com.commerce_pro_backend.payment.enums.TransactionStatus.PARTIALLY_REFUNDED);
            transactionRepository.save(txn);
        } else {
            refund.setStatus(RefundStatus.FAILED);
            refund.setFailureReason(resp.getFailureReason());
        }

        refund.setUpdatedBy(actorId);
        refund = refundRepository.save(refund);
        auditService.log(actorId, resp.isSuccess() ? AuditAction.REFUND_PROCESSED : AuditAction.REFUND_FAILED,
                "RefundRequest", refund.getId(), refund.getRefundRef(), null, null, refund.getStatus().name(), resp.isSuccess());
        return mapper.toRefundDTO(refund);
    }

    private RefundRequest findById(String id) {
        return refundRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("RefundRequest", id));
    }
}
