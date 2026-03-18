package com.commerce_pro_backend.fulfillment.repository;

import com.commerce_pro_backend.fulfillment.entity.PickListSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PickListSequenceRepository extends JpaRepository<PickListSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PickListSequence s WHERE s.sequenceKey = :key")
    Optional<PickListSequence> findByKeyWithLock(@Param("key") String key);
}
