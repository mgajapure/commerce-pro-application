package com.commerce_pro_backend.customer.specification;

import com.commerce_pro_backend.customer.dto.CustomerFilterDTO;
import com.commerce_pro_backend.customer.entity.Customer;
import com.commerce_pro_backend.customer.enums.CustomerStatus;
import com.commerce_pro_backend.customer.enums.CustomerTier;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CustomerSpecification {

    private CustomerSpecification() {}

    public static Specification<Customer> withFilters(CustomerFilterDTO f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();

            if (f.getSearch() != null && !f.getSearch().isBlank()) {
                String like = "%" + f.getSearch().toLowerCase() + "%";
                p.add(cb.or(
                    cb.like(cb.lower(root.get("firstName")), like),
                    cb.like(cb.lower(root.get("lastName")),  like),
                    cb.like(cb.lower(root.get("email")),     like),
                    cb.like(root.get("phone"), "%" + f.getSearch() + "%")
                ));
            }
            if (f.getStatus() != null && !f.getStatus().isBlank())
                p.add(cb.equal(root.get("status"), CustomerStatus.valueOf(f.getStatus())));
            if (f.getTier() != null && !f.getTier().isBlank())
                p.add(cb.equal(root.get("tier"), CustomerTier.valueOf(f.getTier())));
            if (f.getGroupId() != null && !f.getGroupId().isBlank())
                p.add(cb.equal(root.get("group").get("id"), f.getGroupId()));
            if (f.getIsBlacklisted() != null)
                p.add(cb.equal(root.get("isBlacklisted"), f.getIsBlacklisted()));
            if (f.getIsFraudFlagged() != null)
                p.add(cb.equal(root.get("isFraudFlagged"), f.getIsFraudFlagged()));
            if (f.getMarketingOptIn() != null)
                p.add(cb.equal(root.get("marketingOptIn"), f.getMarketingOptIn()));
            if (f.getAcquisitionSource() != null && !f.getAcquisitionSource().isBlank())
                p.add(cb.equal(root.get("acquisitionSource"), f.getAcquisitionSource()));
            if (f.getMinLifetimeSpend() != null)
                p.add(cb.greaterThanOrEqualTo(root.get("lifetimeSpend"), f.getMinLifetimeSpend()));
            if (f.getMaxLifetimeSpend() != null)
                p.add(cb.lessThanOrEqualTo(root.get("lifetimeSpend"), f.getMaxLifetimeSpend()));
            if (f.getMinTotalOrders() != null)
                p.add(cb.greaterThanOrEqualTo(root.get("totalOrders"), f.getMinTotalOrders()));
            if (f.getCreatedFrom() != null)
                p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.getCreatedFrom()));
            if (f.getCreatedTo() != null)
                p.add(cb.lessThanOrEqualTo(root.get("createdAt"), f.getCreatedTo()));
            if (f.getLastOrderFrom() != null)
                p.add(cb.greaterThanOrEqualTo(root.get("lastOrderAt"), f.getLastOrderFrom()));
            if (f.getLastOrderTo() != null)
                p.add(cb.lessThanOrEqualTo(root.get("lastOrderAt"), f.getLastOrderTo()));
            if (f.getTag() != null && !f.getTag().isBlank()) {
                var tagJoin = root.join("tags");
                p.add(cb.equal(tagJoin, f.getTag()));
            }

            query.distinct(true);
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}
