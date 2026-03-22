package com.commerce_pro_backend.finance.specification;

import com.commerce_pro_backend.finance.entity.CustomerInvoice;
import com.commerce_pro_backend.finance.enums.InvoiceStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CustomerInvoiceSpecification {
    private CustomerInvoiceSpecification() {}

    public static Specification<CustomerInvoice> withFilters(
            String search, InvoiceStatus status, String customerId,
            String periodId, LocalDate from, LocalDate to, Boolean overdue) {

        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                preds.add(cb.or(
                    cb.like(cb.lower(root.get("invoiceNumber")), like),
                    cb.like(cb.lower(root.get("customerName")), like),
                    cb.like(cb.lower(root.get("customerEmail")), like),
                    cb.like(cb.lower(root.get("orderNumber")), like)
                ));
            }
            if (status != null)       preds.add(cb.equal(root.get("status"), status));
            if (customerId != null && !customerId.isBlank()) preds.add(cb.equal(root.get("customerId"), customerId));
            if (periodId != null && !periodId.isBlank())     preds.add(cb.equal(root.get("period").get("id"), periodId));
            if (from != null) preds.add(cb.greaterThanOrEqualTo(root.get("dueDate"), from));
            if (to   != null) preds.add(cb.lessThanOrEqualTo(root.get("dueDate"), to));
            if (Boolean.TRUE.equals(overdue)) {
                preds.add(cb.lessThan(root.get("dueDate"), LocalDate.now()));
                preds.add(root.get("status").in(InvoiceStatus.SENT, InvoiceStatus.PARTIALLY_PAID));
            }
            assert query != null; query.distinct(true);
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }
}
