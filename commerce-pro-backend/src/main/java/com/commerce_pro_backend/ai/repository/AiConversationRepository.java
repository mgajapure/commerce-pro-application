package com.commerce_pro_backend.ai.repository;

import com.commerce_pro_backend.ai.entity.AiConversation;
import com.commerce_pro_backend.ai.enums.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation, String> {

    /** Load the active chatbot session for a customer */
    Optional<AiConversation> findByCustomerIdAndSessionTypeAndStatus(
            String customerId, SessionType sessionType, String status);

    /** Load all active NL report sessions for an admin user */
    List<AiConversation> findByUserIdAndSessionTypeAndStatusOrderByLastActiveAtDesc(
            String userId, SessionType sessionType, String status);

    /** Cleanup job — delete conversations whose TTL has elapsed */
    int deleteByExpiresAtBefore(LocalDateTime cutoff);
}
