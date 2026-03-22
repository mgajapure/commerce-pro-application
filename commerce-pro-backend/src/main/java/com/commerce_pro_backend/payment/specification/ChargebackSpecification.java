package com.commerce_pro_backend.payment.specification;

import com.commerce_pro_backend.payment.entity.ChargebackDispute;
import com.commerce_pro_backend.payment.enums.ChargebackStatus;
import org.springframework.data.jpa.domain.Specification;

public class ChargebackSpecification {

    public static Specification<ChargebackDispute> withFilters(ChargebackStatus status, String orderId) {
        return Specification.where(hasStatus(status)).and(hasOrderId(orderId));
    }

    private static Specification<ChargebackDispute> hasStatus(ChargebackStatus s) {
        return s == null ? null : (root, q, cb) -> cb.equal(root.get("status"), s);
    }

    private static Specification<ChargebackDispute> hasOrderId(String orderId) {
        return (orderId == null || orderId.isBlank()) ? null : (root, q, cb) -> cb.equal(root.get("orderId"), orderId);
    }
}
