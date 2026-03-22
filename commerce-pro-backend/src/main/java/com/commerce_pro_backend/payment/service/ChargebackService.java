package com.commerce_pro_backend.payment.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.order.entity.Order;
import com.commerce_pro_backend.order.enums.PaymentStatus;
import com.commerce_pro_backend.order.repository.OrderRepository;
import com.commerce_pro_backend.payment.dto.request.CreateChargebackRequest;
import com.commerce_pro_backend.payment.dto.request.ResolveChargebackRequest;
import com.commerce_pro_backend.payment.dto.request.SubmitEvidenceRequest;
import com.commerce_pro_backend.payment.dto.response.ChargebackDisputeDTO;
import com.commerce_pro_backend.payment.dto.response.ChargebackSummaryDTO;
import com.commerce_pro_backend.payment.entity.ChargebackDispute;
import com.commerce_pro_backend.payment.enums.ChargebackStatus;
import com.commerce_pro_backend.payment.mapper.PaymentMapper;
import com.commerce_pro_backend.payment.repository.ChargebackDisputeRepository;
import com.commerce_pro_backend.payment.specification.ChargebackSpecification;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.service.AuditService;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChargebackService {

    private final ChargebackDisputeRepository chargebackRepository;
    private final PaymentNumberService numberService;
    private final PaymentMapper mapper;
    private final OrderRepository orderRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    // ── READ ──────────────────────────────────────────────────────────────────

    public PageResponse<ChargebackSummaryDTO> list(ChargebackStatus status, String orderId, Pageable pageable) {
        var spec = ChargebackSpecification.withFilters(status, orderId);
        Page<ChargebackDispute> page = chargebackRepository.findAll(spec, pageable);
        return PageResponse.from(page, mapper.toChargebackSummaryList(page.getContent()));
    }

    public ChargebackDisputeDTO getById(String id) {
        return mapper.toChargebackDTO(findById(id));
    }

    public List<ChargebackSummaryDTO> getDeadlineAlerts() {
        LocalDateTime cutoff = LocalDateTime.now().plusDays(3);
        return mapper.toChargebackSummaryList(
                chargebackRepository.findUpcomingDeadlines(LocalDateTime.now(), cutoff));
    }

    // ── WRITE ─────────────────────────────────────────────────────────────────

    @Transactional
    public ChargebackDisputeDTO intake(CreateChargebackRequest req) {
        String actorId = currentUserService.getCurrentUserId();

        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> ApiException.notFound("Order", req.getOrderId()));

        ChargebackDispute dispute = ChargebackDispute.builder()
                .disputeRef(numberService.generateDisputeRef())
                .paymentTransactionId(req.getPaymentTransactionId())
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .customerName(order.getCustomerName())
                .status(ChargebackStatus.RECEIVED)
                .reason(req.getReason())
                .reasonCode(req.getReasonCode())
                .disputedAmount(req.getDisputedAmount())
                .currency(req.getCurrency() != null ? req.getCurrency() : order.getCurrency())
                .gatewayDisputeId(req.getGatewayDisputeId())
                .responseDeadline(req.getResponseDeadline())
                .internalNotes(req.getInternalNotes())
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        dispute = chargebackRepository.save(dispute);

        order.setPaymentStatus(PaymentStatus.CHARGEBACK);
        order.setUpdatedBy(actorId);
        orderRepository.save(order);

        auditService.log(actorId, AuditAction.CHARGEBACK_RECEIVED, "ChargebackDispute",
                dispute.getId(), dispute.getDisputeRef(), null, null, dispute.getStatus().name(), true);
        return mapper.toChargebackDTO(dispute);
    }

    @Transactional
    public ChargebackDisputeDTO submitEvidence(String id, SubmitEvidenceRequest req) {
        String actorId = currentUserService.getCurrentUserId();
        ChargebackDispute dispute = findById(id);
        dispute.setEvidenceNotes(req.getEvidenceNotes());
        dispute.setEvidenceFilePaths(req.getEvidenceFilePathsJson());
        dispute.setEvidenceSubmittedAt(LocalDateTime.now());
        dispute.setStatus(ChargebackStatus.EVIDENCE_SUBMITTED);
        dispute.setUpdatedBy(actorId);
        dispute = chargebackRepository.save(dispute);
        auditService.log(actorId, AuditAction.CHARGEBACK_EVIDENCE_SUBMITTED, "ChargebackDispute",
                dispute.getId(), dispute.getDisputeRef(), null, null, dispute.getStatus().name(), true);
        return mapper.toChargebackDTO(dispute);
    }

    @Transactional
    public ChargebackDisputeDTO accept(String id, String notes) {
        String actorId = currentUserService.getCurrentUserId();
        ChargebackDispute dispute = findById(id);
        dispute.setStatus(ChargebackStatus.ACCEPTED);
        dispute.setResolutionNotes(notes);
        dispute.setResolvedAt(LocalDateTime.now());
        dispute.setIsWon(false);
        dispute.setUpdatedBy(actorId);
        dispute = chargebackRepository.save(dispute);
        auditService.log(actorId, AuditAction.CHARGEBACK_ACCEPTED, "ChargebackDispute",
                dispute.getId(), dispute.getDisputeRef(), null, null, dispute.getStatus().name(), true);
        return mapper.toChargebackDTO(dispute);
    }

    @Transactional
    public ChargebackDisputeDTO resolve(String id, ResolveChargebackRequest req) {
        String actorId = currentUserService.getCurrentUserId();
        ChargebackDispute dispute = findById(id);
        dispute.setStatus(req.isWon() ? ChargebackStatus.WON : ChargebackStatus.LOST);
        dispute.setIsWon(req.isWon());
        dispute.setResolutionNotes(req.getNotes());
        dispute.setResolvedAt(LocalDateTime.now());
        dispute.setUpdatedBy(actorId);
        dispute = chargebackRepository.save(dispute);

        AuditAction action = req.isWon() ? AuditAction.CHARGEBACK_WON : AuditAction.CHARGEBACK_LOST;
        auditService.log(actorId, action, "ChargebackDispute",
                dispute.getId(), dispute.getDisputeRef(), null, null, dispute.getStatus().name(), true);
        return mapper.toChargebackDTO(dispute);
    }

    private ChargebackDispute findById(String id) {
        return chargebackRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ChargebackDispute", id));
    }
}
