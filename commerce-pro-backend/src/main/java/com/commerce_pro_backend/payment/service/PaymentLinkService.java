package com.commerce_pro_backend.payment.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.payment.dto.request.CreatePaymentLinkRequest;
import com.commerce_pro_backend.payment.dto.request.PayViaLinkRequest;
import com.commerce_pro_backend.payment.dto.response.PaymentLinkDTO;
import com.commerce_pro_backend.payment.dto.response.PaymentLinkSummaryDTO;
import com.commerce_pro_backend.payment.dto.response.PaymentTransactionDTO;
import com.commerce_pro_backend.payment.entity.PaymentLink;
import com.commerce_pro_backend.payment.enums.PaymentLinkStatus;
import com.commerce_pro_backend.payment.mapper.PaymentMapper;
import com.commerce_pro_backend.payment.repository.PaymentLinkRepository;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentLinkService {

    private final PaymentLinkRepository linkRepository;
    private final PaymentMapper mapper;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public PageResponse<PaymentLinkSummaryDTO> list(PaymentLinkStatus status, Pageable pageable) {
        Page<PaymentLink> page = status != null
                ? linkRepository.findByStatus(status, pageable)
                : linkRepository.findAll(pageable);
        return PageResponse.from(page, mapper.toLinkSummaryList(page.getContent()));
    }

    public PaymentLinkDTO getById(String id) {
        return mapper.toLinkDTO(findById(id));
    }

    public PaymentLinkDTO getBySlug(String slug) {
        PaymentLink link = linkRepository.findBySlug(slug)
                .orElseThrow(() -> ApiException.notFound("PaymentLink", slug));
        if (link.isExpired()) {
            link.setStatus(PaymentLinkStatus.EXPIRED);
            linkRepository.save(link);
        }
        return mapper.toLinkDTO(link);
    }

    @Transactional
    public PaymentLinkDTO create(CreatePaymentLinkRequest req) {
        String actorId = currentUserService.getCurrentUserId();
        String slug = generateUniqueSlug();

        PaymentLink link = PaymentLink.builder()
                .slug(slug)
                .title(req.getTitle())
                .orderId(req.getOrderId())
                .customerEmail(req.getCustomerEmail())
                .status(PaymentLinkStatus.ACTIVE)
                .amount(req.getAmount())
                .currency(req.getCurrency() != null ? req.getCurrency() : "USD")
                .isOneTime(req.getIsOneTime() == null || req.getIsOneTime())
                .maxUses(req.getMaxUses())
                .expiresAt(req.getExpiresAt())
                .description(req.getDescription())
                .metadata(req.getMetadata())
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        link = linkRepository.save(link);
        auditService.log(actorId, AuditAction.PAYMENT_LINK_CREATED, "PaymentLink",
                link.getId(), link.getSlug(), null, null, link.getStatus().name(), true);
        return mapper.toLinkDTO(link);
    }

    @Transactional
    public PaymentLinkDTO deactivate(String id) {
        String actorId = currentUserService.getCurrentUserId();
        PaymentLink link = findById(id);
        link.setStatus(PaymentLinkStatus.CANCELLED);
        link.setUpdatedBy(actorId);
        link = linkRepository.save(link);
        auditService.log(actorId, AuditAction.PAYMENT_LINK_DEACTIVATED, "PaymentLink",
                link.getId(), link.getSlug(), null, PaymentLinkStatus.ACTIVE.name(), PaymentLinkStatus.CANCELLED.name(), true);
        return mapper.toLinkDTO(link);
    }

    /** Expire stale links — called by a scheduled task or manually */
    @Transactional
    public void expireStaleLinks() {
        linkRepository.findExpiredActiveLinks(LocalDateTime.now())
                .forEach(link -> {
                    link.setStatus(PaymentLinkStatus.EXPIRED);
                    linkRepository.save(link);
                });
    }

    private PaymentLink findById(String id) {
        return linkRepository.findById(id).orElseThrow(() -> ApiException.notFound("PaymentLink", id));
    }

    private String generateUniqueSlug() {
        String slug;
        do {
            slug = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        } while (linkRepository.existsBySlug(slug));
        return slug;
    }
}
