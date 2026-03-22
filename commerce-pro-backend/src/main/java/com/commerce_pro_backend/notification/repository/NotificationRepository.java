package com.commerce_pro_backend.notification.repository;

import com.commerce_pro_backend.notification.entity.Notification;
import com.commerce_pro_backend.notification.enums.NotificationPriority;
import com.commerce_pro_backend.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);

    long countByUserIdAndIsReadFalse(String userId);

    long countByUserId(String userId);

    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.userId = :userId GROUP BY n.type")
    List<Object[]> countByUserIdGroupedByType(@Param("userId") String userId);

    @Query("SELECT n.priority, COUNT(n) FROM Notification n WHERE n.userId = :userId GROUP BY n.priority")
    List<Object[]> countByUserIdGroupedByPriority(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") String userId, @Param("readAt") LocalDateTime readAt);
}
