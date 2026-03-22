package com.commerce_pro_backend.finance.specification;

import com.commerce_pro_backend.finance.entity.JournalEntry;
import com.commerce_pro_backend.finance.enums.JournalEntryType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JournalEntrySpecification {
    private JournalEntrySpecification() {}

    public static Specification<JournalEntry> withFilters(
            String search, JournalEntryType entryType, String periodId,
            String status, LocalDate from, LocalDate to) {

        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                preds.add(cb.or(
                    cb.like(cb.lower(root.get("entryRef")), like),
                    cb.like(cb.lower(root.get("description")), like),
                    cb.like(cb.lower(root.get("reference")), like)
                ));
            }
            if (entryType != null)    preds.add(cb.equal(root.get("entryType"), entryType));
            if (status != null && !status.isBlank()) preds.add(cb.equal(root.get("status"), status));
            if (periodId != null && !periodId.isBlank()) preds.add(cb.equal(root.get("period").get("id"), periodId));
            if (from != null) preds.add(cb.greaterThanOrEqualTo(root.get("entryDate"), from));
            if (to   != null) preds.add(cb.lessThanOrEqualTo(root.get("entryDate"), to));
            assert query != null; query.distinct(true);
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }
}
