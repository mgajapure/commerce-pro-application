package com.commerce_pro_backend.payment.specification;

import com.commerce_pro_backend.payment.entity.PaymentTransaction;
import com.commerce_pro_backend.payment.enums.GatewayProvider;
import com.commerce_pro_backend.payment.enums.TransactionStatus;
import com.commerce_pro_backend.payment.enums.TransactionType;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;

public class PaymentTransactionSpecification {

    public static Specification<PaymentTransaction> withFilters(
            String search, TransactionStatus status, TransactionType type,
            GatewayProvider gateway, String orderId, String customerId,
            Boolean isFlagged, LocalDateTime from, LocalDateTime to) {

        return Specification.where(hasSearch(search))
                .and(hasStatus(status))
                .and(hasType(type))
                .and(hasGateway(gateway))
                .and(hasOrderId(orderId))
                .and(hasCustomerId(customerId))
                .and(hasFlagged(isFlagged))
                .and(createdAfter(from))
                .and(createdBefore(to));
    }

    private static Specification<PaymentTransaction> hasSearch(String search) {
        if (search == null || search.isBlank()) return null;
        String like = "%" + search.toLowerCase() + "%";
        return (root, q, cb) -> cb.or(
                cb.like(cb.lower(root.get("transactionRef")), like),
                cb.like(cb.lower(root.get("orderNumber")), like),
                cb.like(cb.lower(root.get("customerName")), like),
                cb.like(cb.lower(root.get("customerEmail")), like)
        );
    }

    private static Specification<PaymentTransaction> hasStatus(TransactionStatus s) {
        return s == null ? null : (root, q, cb) -> cb.equal(root.get("status"), s);
    }

    private static Specification<PaymentTransaction> hasType(TransactionType t) {
        return t == null ? null : (root, q, cb) -> cb.equal(root.get("transactionType"), t);
    }

    private static Specification<PaymentTransaction> hasGateway(GatewayProvider g) {
        return g == null ? null : (root, q, cb) -> cb.equal(root.get("gatewayProvider"), g);
    }

    private static Specification<PaymentTransaction> hasOrderId(String orderId) {
        return (orderId == null || orderId.isBlank()) ? null : (root, q, cb) -> cb.equal(root.get("orderId"), orderId);
    }

    private static Specification<PaymentTransaction> hasCustomerId(String customerId) {
        return (customerId == null || customerId.isBlank()) ? null : (root, q, cb) -> cb.equal(root.get("customerId"), customerId);
    }

    private static Specification<PaymentTransaction> hasFlagged(Boolean flagged) {
        return flagged == null ? null : (root, q, cb) -> cb.equal(root.get("isFlagged"), flagged);
    }

    private static Specification<PaymentTransaction> createdAfter(LocalDateTime from) {
        return from == null ? null : (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    private static Specification<PaymentTransaction> createdBefore(LocalDateTime to) {
        return to == null ? null : (root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
