package com.commerce_pro_backend.payment.repository;

import com.commerce_pro_backend.payment.entity.PaymentSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentSequenceRepository extends JpaRepository<PaymentSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PaymentSequence s WHERE s.sequenceKey = :key")
    Optional<PaymentSequence> findByKeyWithLock(@Param("key") String key);
}
