package com.commerce_pro_backend.payment.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.payment.dto.request.CreatePayoutRequest;
import com.commerce_pro_backend.payment.dto.response.PayoutDTO;
import com.commerce_pro_backend.payment.dto.response.PayoutSummaryDTO;
import com.commerce_pro_backend.payment.entity.Payout;
import com.commerce_pro_backend.payment.entity.PayoutItem;
import com.commerce_pro_backend.payment.enums.PayoutStatus;
import com.commerce_pro_backend.payment.enums.TransactionStatus;
import com.commerce_pro_backend.payment.enums.TransactionType;
import com.commerce_pro_backend.payment.mapper.PaymentMapper;
import com.commerce_pro_backend.payment.repository.PayoutItemRepository;
import com.commerce_pro_backend.payment.repository.PayoutRepository;
import com.commerce_pro_backend.payment.repository.PaymentTransactionRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayoutService {

    private final PayoutRepository payoutRepository;
    private final PayoutItemRepository payoutItemRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PayoutNumberService numberService;
    private final PaymentMapper mapper;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public PageResponse<PayoutSummaryDTO> list(PayoutStatus status, Pageable pageable) {
        Page<Payout> page = status != null
                ? payoutRepository.findByStatus(status, pageable)
                : payoutRepository.findAll(pageable);
        return PageResponse.from(page, mapper.toPayoutSummaryList(page.getContent()));
    }

    public PayoutDTO getById(String id) {
        return mapper.toPayoutDTO(findById(id));
    }

    @Transactional
    public PayoutDTO create(CreatePayoutRequest req) {
        String actorId = currentUserService.getCurrentUserId();

        // Collect all settled transactions not yet included in a payout
        var settled = transactionRepository.findByStatusAndTransactionType(
                TransactionStatus.SETTLED, TransactionType.CHARGE);

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;

        Payout payout = Payout.builder()
                .payoutRef(numberService.generatePayoutRef())
                .status(PayoutStatus.PENDING)
                .periodStart(req.getPeriodStart())
                .periodEnd(req.getPeriodEnd())
                .currency("USD")
                .bankAccountName(req.getBankAccountName())
                .bankAccountLast4(req.getBankAccountLast4())
                .notes(req.getNotes())
                .totalAmount(BigDecimal.ZERO)
                .totalFees(BigDecimal.ZERO)
                .netAmount(BigDecimal.ZERO)
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        payout = payoutRepository.save(payout);

        for (var txn : settled) {
            if (payoutItemRepository.existsByPaymentTransactionId(txn.getId())) continue;
            if (txn.getSettledAt() == null) continue;
            if (txn.getSettledAt().isBefore(req.getPeriodStart()) || txn.getSettledAt().isAfter(req.getPeriodEnd())) continue;

            BigDecimal fee = txn.getGatewayFee() != null ? txn.getGatewayFee() : BigDecimal.ZERO;
            BigDecimal net = txn.getAmount().subtract(fee);
            totalGross = totalGross.add(txn.getAmount());
            totalFees = totalFees.add(fee);

            PayoutItem item = PayoutItem.builder()
                    .payout(payout)
                    .paymentTransactionId(txn.getId())
                    .transactionRef(txn.getTransactionRef())
                    .orderId(txn.getOrderId())
                    .orderNumber(txn.getOrderNumber())
                    .grossAmount(txn.getAmount())
                    .feeAmount(fee)
                    .netAmount(net)
                    .build();
            payoutItemRepository.save(item);
        }

        payout.setTotalAmount(totalGross);
        payout.setTotalFees(totalFees);
        payout.setNetAmount(totalGross.subtract(totalFees));
        payout = payoutRepository.save(payout);

        auditService.log(actorId, AuditAction.PAYOUT_CREATED, "Payout",
                payout.getId(), payout.getPayoutRef(), null, null, payout.getStatus().name(), true);
        return mapper.toPayoutDTO(payout);
    }

    @Transactional
    public PayoutDTO approve(String id) {
        String actorId = currentUserService.getCurrentUserId();
        Payout payout = findById(id);
        if (payout.getStatus() != PayoutStatus.PENDING)
            throw ApiException.badRequest("Payout can only be approved from PENDING status");
        payout.setStatus(PayoutStatus.APPROVED);
        payout.setUpdatedBy(actorId);
        payout = payoutRepository.save(payout);
        auditService.log(actorId, AuditAction.PAYOUT_APPROVED, "Payout",
                payout.getId(), payout.getPayoutRef(), null, PayoutStatus.PENDING.name(), PayoutStatus.APPROVED.name(), true);
        return mapper.toPayoutDTO(payout);
    }

    @Transactional
    public PayoutDTO initiate(String id) {
        String actorId = currentUserService.getCurrentUserId();
        Payout payout = findById(id);
        if (payout.getStatus() != PayoutStatus.APPROVED)
            throw ApiException.badRequest("Payout must be APPROVED before initiation");
        payout.setStatus(PayoutStatus.IN_TRANSIT);
        payout.setInitiatedAt(LocalDateTime.now());
        payout.setEstimatedArrival(LocalDateTime.now().plusDays(2));
        payout.setUpdatedBy(actorId);
        payout = payoutRepository.save(payout);
        auditService.log(actorId, AuditAction.PAYOUT_INITIATED, "Payout",
                payout.getId(), payout.getPayoutRef(), null, null, payout.getStatus().name(), true);
        return mapper.toPayoutDTO(payout);
    }

    @Transactional
    public PayoutDTO complete(String id) {
        String actorId = currentUserService.getCurrentUserId();
        Payout payout = findById(id);
        payout.setStatus(PayoutStatus.COMPLETED);
        payout.setCompletedAt(LocalDateTime.now());
        payout.setUpdatedBy(actorId);
        payout = payoutRepository.save(payout);
        auditService.log(actorId, AuditAction.PAYOUT_COMPLETED, "Payout",
                payout.getId(), payout.getPayoutRef(), null, null, PayoutStatus.COMPLETED.name(), true);
        return mapper.toPayoutDTO(payout);
    }

    @Transactional
    public PayoutDTO cancel(String id, String reason) {
        String actorId = currentUserService.getCurrentUserId();
        Payout payout = findById(id);
        if (payout.getStatus() == PayoutStatus.COMPLETED || payout.getStatus() == PayoutStatus.IN_TRANSIT)
            throw ApiException.badRequest("Cannot cancel a payout that is IN_TRANSIT or COMPLETED");
        payout.setStatus(PayoutStatus.CANCELLED);
        payout.setNotes(reason);
        payout.setUpdatedBy(actorId);
        payout = payoutRepository.save(payout);
        auditService.log(actorId, AuditAction.PAYOUT_CANCELLED, "Payout",
                payout.getId(), payout.getPayoutRef(), null, null, PayoutStatus.CANCELLED.name(), true);
        return mapper.toPayoutDTO(payout);
    }

    private Payout findById(String id) {
        return payoutRepository.findById(id).orElseThrow(() -> ApiException.notFound("Payout", id));
    }
}
