package com.commerce_pro_backend.payment.repository;

import com.commerce_pro_backend.payment.entity.PayoutSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayoutSequenceRepository extends JpaRepository<PayoutSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PayoutSequence s WHERE s.sequenceKey = :key")
    Optional<PayoutSequence> findByKeyWithLock(@Param("key") String key);
}
