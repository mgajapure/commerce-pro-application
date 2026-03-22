package com.commerce_pro_backend.payment.repository;

import com.commerce_pro_backend.payment.entity.Payout;
import com.commerce_pro_backend.payment.enums.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, String>, JpaSpecificationExecutor<Payout> {

    Optional<Payout> findByPayoutRef(String payoutRef);

    Page<Payout> findByStatus(PayoutStatus status, Pageable pageable);

    List<Payout> findByStatus(PayoutStatus status);

    @Query("SELECT COALESCE(SUM(p.netAmount), 0) FROM Payout p WHERE p.status IN ('PENDING','APPROVED')")
    BigDecimal sumPendingPayoutAmount();
}
