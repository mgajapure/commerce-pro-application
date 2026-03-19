package com.commerce_pro_backend.customer.repository;

import com.commerce_pro_backend.customer.entity.CustomerCommunicationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CustomerCommunicationLogRepository extends JpaRepository<CustomerCommunicationLog, String> {
    Page<CustomerCommunicationLog> findByCustomerId(String customerId, Pageable pageable);
    long countByCustomerId(String customerId);

    @Query("SELECT c FROM CustomerCommunicationLog c WHERE c.customer.id = :id AND c.requiresFollowUp = true AND c.isResolved = false")
    List<CustomerCommunicationLog> findPendingFollowUps(@Param("id") String customerId);

    @Query("SELECT c FROM CustomerCommunicationLog c WHERE c.customer.id = :id AND c.followUpAt <= :now AND c.isResolved = false")
    List<CustomerCommunicationLog> findOverdueFollowUps(@Param("id") String customerId, @Param("now") LocalDateTime now);
}
