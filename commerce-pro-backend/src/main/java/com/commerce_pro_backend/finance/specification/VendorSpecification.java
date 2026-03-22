package com.commerce_pro_backend.finance.specification;

import com.commerce_pro_backend.finance.entity.Vendor;
import com.commerce_pro_backend.finance.enums.VendorStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

public class VendorSpecification {
    private VendorSpecification() {}

    public static Specification<Vendor> withFilters(String search, VendorStatus status, String category) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                preds.add(cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("vendorCode")), like),
                    cb.like(cb.lower(root.get("email")), like),
                    cb.like(cb.lower(root.get("companyName")), like)
                ));
            }
            if (status   != null && !status.name().isBlank()) preds.add(cb.equal(root.get("status"), status));
            if (category != null && !category.isBlank())      preds.add(cb.equal(root.get("category"), category));
            assert query != null; query.distinct(true);
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }
}
