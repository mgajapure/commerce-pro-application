package com.commerce_pro_backend.fulfillment.specification;

import com.commerce_pro_backend.fulfillment.entity.Shipment;
import com.commerce_pro_backend.fulfillment.enums.ShipmentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ShipmentSpecification {

    private ShipmentSpecification() {}

    public static Specification<Shipment> withFilters(
            String search,
            String status,
            String carrierId,
            String orderId,
            String createdFrom,
            String createdTo) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("shipmentNumber")), like),
                        cb.like(cb.lower(root.get("trackingNumber")), like),
                        cb.like(cb.lower(root.get("orderNumber")), like),
                        cb.like(cb.lower(root.get("recipientName")), like)
                ));
            }

            if (status != null && !status.isBlank()) {
                try {
                    predicates.add(cb.equal(root.get("status"), ShipmentStatus.valueOf(status)));
                } catch (IllegalArgumentException ignored) {}
            }

            if (carrierId != null && !carrierId.isBlank()) {
                predicates.add(cb.equal(root.get("carrierId"), carrierId));
            }

            if (orderId != null && !orderId.isBlank()) {
                predicates.add(cb.equal(root.get("orderId"), orderId));
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
