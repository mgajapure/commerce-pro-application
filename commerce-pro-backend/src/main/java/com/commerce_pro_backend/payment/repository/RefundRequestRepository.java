package com.commerce_pro_backend.payment.repository;

import com.commerce_pro_backend.payment.entity.RefundRequest;
import com.commerce_pro_backend.payment.enums.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, String>, JpaSpecificationExecutor<RefundRequest> {

    Optional<RefundRequest> findByRefundRef(String refundRef);

    List<RefundRequest> findByOrderId(String orderId);

    Page<RefundRequest> findByStatus(RefundStatus status, Pageable pageable);

    long countByStatus(RefundStatus status);

    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM RefundRequest r WHERE r.status IN ('PENDING','UNDER_REVIEW','APPROVED')")
    BigDecimal sumPendingRefundAmount();

    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM RefundRequest r WHERE r.status = 'COMPLETED' AND r.orderId = :orderId")
    BigDecimal sumCompletedRefundsForOrder(@Param("orderId") String orderId);
}
