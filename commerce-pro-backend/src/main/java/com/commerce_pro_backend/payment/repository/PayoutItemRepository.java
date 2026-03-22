package com.commerce_pro_backend.payment.repository;

import com.commerce_pro_backend.payment.entity.PayoutItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayoutItemRepository extends JpaRepository<PayoutItem, String> {
    Page<PayoutItem> findByPayoutId(String payoutId, Pageable pageable);
    boolean existsByPaymentTransactionId(String paymentTransactionId);
}
