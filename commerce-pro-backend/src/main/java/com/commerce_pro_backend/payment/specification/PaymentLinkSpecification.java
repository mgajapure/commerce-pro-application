package com.commerce_pro_backend.payment.specification;

import com.commerce_pro_backend.payment.entity.PaymentLink;
import com.commerce_pro_backend.payment.enums.PaymentLinkStatus;
import org.springframework.data.jpa.domain.Specification;

public class PaymentLinkSpecification {

    public static Specification<PaymentLink> withFilters(PaymentLinkStatus status, String orderId) {
        return Specification.where(hasStatus(status)).and(hasOrderId(orderId));
    }

    private static Specification<PaymentLink> hasStatus(PaymentLinkStatus s) {
        return s == null ? null : (root, q, cb) -> cb.equal(root.get("status"), s);
    }

    private static Specification<PaymentLink> hasOrderId(String orderId) {
        return (orderId == null || orderId.isBlank()) ? null : (root, q, cb) -> cb.equal(root.get("orderId"), orderId);
    }
}
