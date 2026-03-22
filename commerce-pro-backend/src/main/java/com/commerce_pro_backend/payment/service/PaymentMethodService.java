package com.commerce_pro_backend.payment.service;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.payment.dto.request.AddPaymentMethodRequest;
import com.commerce_pro_backend.payment.dto.response.PaymentMethodDTO;
import com.commerce_pro_backend.payment.entity.PaymentMethod;
import com.commerce_pro_backend.payment.mapper.PaymentMapper;
import com.commerce_pro_backend.payment.repository.PaymentMethodRepository;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.service.AuditService;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentMethodService {

    private final PaymentMethodRepository methodRepository;
    private final PaymentMapper mapper;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public List<PaymentMethodDTO> getByCustomerId(String customerId) {
        return mapper.toMethodDTOList(methodRepository.findByCustomerIdAndIsActiveTrue(customerId));
    }

    @Transactional
    public PaymentMethodDTO add(AddPaymentMethodRequest req) {
        String actorId = currentUserService.getCurrentUserId();
        PaymentMethod method = PaymentMethod.builder()
                .customerId(req.getCustomerId())
                .methodType(req.getMethodType())
                .gatewayProvider(req.getGatewayProvider())
                .gatewayToken(req.getGatewayToken())
                .cardLast4(req.getCardLast4())
                .cardBrand(req.getCardBrand())
                .cardExpMonth(req.getCardExpMonth())
                .cardExpYear(req.getCardExpYear())
                .cardHolderName(req.getCardHolderName())
                .accountLabel(req.getAccountLabel())
                .isDefault(req.getIsDefault() != null && req.getIsDefault())
                .createdBy(actorId)
                .build();

        if (Boolean.TRUE.equals(req.getIsDefault())) {
            clearExistingDefault(req.getCustomerId());
        }

        method = methodRepository.save(method);
        auditService.log(actorId, AuditAction.PAYMENT_METHOD_ADDED, "PaymentMethod",
                method.getId(), method.getMethodType().name(), null, null, "ADDED", true);
        return mapper.toMethodDTO(method);
    }

    @Transactional
    public void remove(String id) {
        String actorId = currentUserService.getCurrentUserId();
        PaymentMethod method = findById(id);
        method.setIsActive(false);
        methodRepository.save(method);
        auditService.log(actorId, AuditAction.PAYMENT_METHOD_REMOVED, "PaymentMethod",
                method.getId(), method.getMethodType().name(), null, null, "REMOVED", true);
    }

    @Transactional
    public PaymentMethodDTO setDefault(String id) {
        String actorId = currentUserService.getCurrentUserId();
        PaymentMethod method = findById(id);
        clearExistingDefault(method.getCustomerId());
        method.setIsDefault(true);
        method = methodRepository.save(method);
        return mapper.toMethodDTO(method);
    }

    private void clearExistingDefault(String customerId) {
        methodRepository.findByCustomerIdAndIsDefaultTrueAndIsActiveTrue(customerId)
                .ifPresent(m -> { m.setIsDefault(false); methodRepository.save(m); });
    }

    private PaymentMethod findById(String id) {
        return methodRepository.findById(id).orElseThrow(() -> ApiException.notFound("PaymentMethod", id));
    }
}
