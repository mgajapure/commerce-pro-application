package com.commerce_pro_backend.customer.repository;

import com.commerce_pro_backend.customer.entity.SavedCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SavedCartRepository extends JpaRepository<SavedCart, String> {
    List<SavedCart> findByCustomerId(String customerId);

    @Query("SELECT c FROM SavedCart c WHERE c.isAbandoned = false AND c.updatedAt < :threshold")
    List<SavedCart> findAbandoned(@Param("threshold") LocalDateTime threshold);
}
