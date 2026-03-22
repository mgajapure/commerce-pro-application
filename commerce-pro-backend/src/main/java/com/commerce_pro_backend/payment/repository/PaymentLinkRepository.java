package com.commerce_pro_backend.payment.repository;

import com.commerce_pro_backend.payment.entity.PaymentLink;
import com.commerce_pro_backend.payment.enums.PaymentLinkStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentLinkRepository extends JpaRepository<PaymentLink, String>, JpaSpecificationExecutor<PaymentLink> {

    Optional<PaymentLink> findBySlug(String slug);

    Optional<PaymentLink> findByOrderId(String orderId);

    Page<PaymentLink> findByStatus(PaymentLinkStatus status, Pageable pageable);

    long countByStatus(PaymentLinkStatus status);

    @Query("SELECT l FROM PaymentLink l WHERE l.status = 'ACTIVE' AND l.expiresAt < :now")
    List<PaymentLink> findExpiredActiveLinks(@Param("now") LocalDateTime now);

    boolean existsBySlug(String slug);
}
