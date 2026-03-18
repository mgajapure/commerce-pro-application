package com.commerce_pro_backend.order.repository;

import com.commerce_pro_backend.order.entity.OrderSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderSequenceRepository extends JpaRepository<OrderSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM OrderSequence s WHERE s.sequenceKey = :key")
    Optional<OrderSequence> findByKeyWithLock(@Param("key") String key);
}
