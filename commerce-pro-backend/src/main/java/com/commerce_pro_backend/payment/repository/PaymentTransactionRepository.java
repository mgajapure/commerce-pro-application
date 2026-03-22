package com.commerce_pro_backend.payment.repository;

import com.commerce_pro_backend.payment.entity.PaymentTransaction;
import com.commerce_pro_backend.payment.enums.TransactionStatus;
import com.commerce_pro_backend.payment.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction, String>, JpaSpecificationExecutor<PaymentTransaction> {

    Optional<PaymentTransaction> findByTransactionRef(String transactionRef);

    Page<PaymentTransaction> findByOrderId(String orderId, Pageable pageable);

    Page<PaymentTransaction> findByCustomerId(String customerId, Pageable pageable);

    List<PaymentTransaction> findByOrderId(String orderId);

    Page<PaymentTransaction> findByStatus(TransactionStatus status, Pageable pageable);

    List<PaymentTransaction> findByStatusAndTransactionType(TransactionStatus status, TransactionType type);

    @Query("SELECT t FROM PaymentTransaction t WHERE t.status = 'AUTHORIZED' AND t.expiresAt < :cutoff")
    List<PaymentTransaction> findExpiredAuthorizations(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT t FROM PaymentTransaction t WHERE t.status = 'AUTHORIZED' AND t.expiresAt BETWEEN :now AND :cutoff")
    List<PaymentTransaction> findExpiringAuthorizations(@Param("now") LocalDateTime now, @Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT COUNT(t) FROM PaymentTransaction t WHERE t.createdAt >= :since")
    long countTransactionsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE t.transactionType = 'CHARGE' AND t.status IN ('CAPTURED','SETTLED') AND t.createdAt >= :since")
    BigDecimal sumRevenueSince(@Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE t.transactionType = 'CHARGE' AND t.status IN ('CAPTURED','SETTLED')")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE t.status = 'AUTHORIZED'")
    BigDecimal sumPendingCaptureAmount();

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE t.status = 'SETTLED' AND t.paidOut = false")
    default BigDecimal sumUnpaidSettledAmount() { return BigDecimal.ZERO; }

    boolean existsByTransactionRef(String transactionRef);

    @Query("SELECT t FROM PaymentTransaction t WHERE t.status = 'SETTLED' AND t.settledAt BETWEEN :from AND :to")
    List<PaymentTransaction> findSettledBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
