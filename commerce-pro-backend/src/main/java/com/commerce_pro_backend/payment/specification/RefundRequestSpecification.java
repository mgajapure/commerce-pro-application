package com.commerce_pro_backend.payment.specification;

import com.commerce_pro_backend.payment.entity.RefundRequest;
import com.commerce_pro_backend.payment.enums.RefundStatus;
import org.springframework.data.jpa.domain.Specification;

public class RefundRequestSpecification {

    public static Specification<RefundRequest> withFilters(RefundStatus status, String orderId) {
        return Specification.where(hasStatus(status)).and(hasOrderId(orderId));
    }

    private static Specification<RefundRequest> hasStatus(RefundStatus s) {
        return s == null ? null : (root, q, cb) -> cb.equal(root.get("status"), s);
    }

    private static Specification<RefundRequest> hasOrderId(String orderId) {
        return (orderId == null || orderId.isBlank()) ? null : (root, q, cb) -> cb.equal(root.get("orderId"), orderId);
    }
}
