package com.commerce_pro_backend.finance.specification;

import com.commerce_pro_backend.finance.entity.Expense;
import com.commerce_pro_backend.finance.enums.ExpenseStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExpenseSpecification {
    private ExpenseSpecification() {}

    public static Specification<Expense> withFilters(
            String search, ExpenseStatus status, String categoryId,
            String periodId, LocalDate from, LocalDate to) {

        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                preds.add(cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("expenseRef")), like),
                    cb.like(cb.lower(root.get("vendorName")), like)
                ));
            }
            if (status != null)      preds.add(cb.equal(root.get("status"), status));
            if (categoryId != null && !categoryId.isBlank()) preds.add(cb.equal(root.get("category").get("id"), categoryId));
            if (periodId   != null && !periodId.isBlank())   preds.add(cb.equal(root.get("period").get("id"), periodId));
            if (from != null) preds.add(cb.greaterThanOrEqualTo(root.get("expenseDate"), from));
            if (to   != null) preds.add(cb.lessThanOrEqualTo(root.get("expenseDate"), to));

            assert query != null; query.distinct(true);
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }
}
