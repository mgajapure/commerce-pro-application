package com.commerce_pro_backend.payment.specification;

import com.commerce_pro_backend.payment.entity.Payout;
import com.commerce_pro_backend.payment.enums.PayoutStatus;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;

public class PayoutSpecification {

    public static Specification<Payout> withFilters(PayoutStatus status, LocalDateTime from, LocalDateTime to) {
        return Specification.where(hasStatus(status)).and(periodAfter(from)).and(periodBefore(to));
    }

    private static Specification<Payout> hasStatus(PayoutStatus s) {
        return s == null ? null : (root, q, cb) -> cb.equal(root.get("status"), s);
    }

    private static Specification<Payout> periodAfter(LocalDateTime from) {
        return from == null ? null : (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("periodStart"), from);
    }

    private static Specification<Payout> periodBefore(LocalDateTime to) {
        return to == null ? null : (root, q, cb) -> cb.lessThanOrEqualTo(root.get("periodEnd"), to);
    }
}
