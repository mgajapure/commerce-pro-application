package com.commerce_pro_backend.fulfillment.specification;

import com.commerce_pro_backend.fulfillment.entity.PickList;
import com.commerce_pro_backend.fulfillment.enums.PickListStatus;
import com.commerce_pro_backend.fulfillment.enums.PickListType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PickListSpecification {

    private PickListSpecification() {}

    public static Specification<PickList> withFilters(
            String search,
            String status,
            String type,
            String assignedToUserId,
            String warehouseId,
            String createdFrom,
            String createdTo) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("pickListNumber")), like),
                        cb.like(cb.lower(root.get("assignedToName")), like),
                        cb.like(cb.lower(root.get("warehouseName")), like)
                ));
            }

            if (status != null && !status.isBlank()) {
                try {
                    predicates.add(cb.equal(root.get("status"), PickListStatus.valueOf(status)));
                } catch (IllegalArgumentException ignored) {}
            }

            if (type != null && !type.isBlank()) {
                try {
                    predicates.add(cb.equal(root.get("type"), PickListType.valueOf(type)));
                } catch (IllegalArgumentException ignored) {}
            }

            if (assignedToUserId != null && !assignedToUserId.isBlank()) {
                predicates.add(cb.equal(root.get("assignedToUserId"), assignedToUserId));
            }

            if (warehouseId != null && !warehouseId.isBlank()) {
                predicates.add(cb.equal(root.get("warehouseId"), warehouseId));
            }

            if (createdFrom != null && !createdFrom.isBlank()) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"), LocalDate.parse(createdFrom).atStartOfDay()));
            }

            if (createdTo != null && !createdTo.isBlank()) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("createdAt"), LocalDate.parse(createdTo).atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
