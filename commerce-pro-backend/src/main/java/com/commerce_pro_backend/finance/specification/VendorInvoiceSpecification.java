package com.commerce_pro_backend.finance.specification;

import com.commerce_pro_backend.finance.entity.VendorInvoice;
import com.commerce_pro_backend.finance.enums.VendorInvoiceStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VendorInvoiceSpecification {
    private VendorInvoiceSpecification() {}

    public static Specification<VendorInvoice> withFilters(
            String search, VendorInvoiceStatus status, String vendorId,
            String periodId, LocalDate from, LocalDate to) {

        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                preds.add(cb.or(
                    cb.like(cb.lower(root.get("vendorInvoiceNumber")), like),
                    cb.like(cb.lower(root.get("vendorReference")), like),
                    cb.like(cb.lower(root.get("description")), like)
                ));
            }
            if (status != null)       preds.add(cb.equal(root.get("status"), status));
            if (vendorId != null && !vendorId.isBlank()) preds.add(cb.equal(root.get("vendor").get("id"), vendorId));
            if (periodId != null && !periodId.isBlank()) preds.add(cb.equal(root.get("period").get("id"), periodId));
            if (from != null) preds.add(cb.greaterThanOrEqualTo(root.get("dueDate"), from));
            if (to   != null) preds.add(cb.lessThanOrEqualTo(root.get("dueDate"), to));

            assert query != null; query.distinct(true);
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }
}
