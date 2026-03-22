package com.commerce_pro_backend.notification.service;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.notification.dto.CreateNotificationRequest;
import com.commerce_pro_backend.notification.dto.NotificationDTO;
import com.commerce_pro_backend.notification.dto.NotificationStatsDTO;
import com.commerce_pro_backend.notification.entity.Notification;
import com.commerce_pro_backend.notification.enums.NotificationChannel;
import com.commerce_pro_backend.notification.enums.NotificationPriority;
import com.commerce_pro_backend.notification.enums.NotificationType;
import com.commerce_pro_backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // ── List notifications (paginated) ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDTO);
    }

    // ── Unread notifications ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // ── Stats ───────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public NotificationStatsDTO getStats(String userId) {
        long total = notificationRepository.countByUserId(userId);
        long unread = notificationRepository.countByUserIdAndIsReadFalse(userId);

        Map<String, Integer> byType = new HashMap<>();
        for (Object[] row : notificationRepository.countByUserIdGroupedByType(userId)) {
            byType.put(row[0].toString(), ((Long) row[1]).intValue());
        }

        Map<String, Integer> byPriority = new HashMap<>();
        for (Object[] row : notificationRepository.countByUserIdGroupedByPriority(userId)) {
            byPriority.put(row[0].toString(), ((Long) row[1]).intValue());
        }

        return NotificationStatsDTO.builder()
                .total(total)
                .unread(unread)
                .byType(byType)
                .byPriority(byPriority)
                .build();
    }

    // ── Create notification ─────────────────────────────────────────────────────

    @Transactional
    public NotificationDTO createNotification(CreateNotificationRequest request) {
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .channel(request.getChannel() != null ? request.getChannel() : NotificationChannel.IN_APP)
                .priority(request.getPriority() != null ? request.getPriority() : NotificationPriority.NORMAL)
                .link(request.getLink())
                .metadata(request.getMetadata())
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created: id={}, userId={}, type={}", saved.getId(), saved.getUserId(), saved.getType());
        return toDTO(saved);
    }

    // ── Mark as read ────────────────────────────────────────────────────────────

    @Transactional
    public NotificationDTO markAsRead(String id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Notification", id));

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return toDTO(notification);
    }

    // ── Mark all as read ────────────────────────────────────────────────────────

    @Transactional
    public int markAllAsRead(String userId) {
        int count = notificationRepository.markAllAsReadByUserId(userId, LocalDateTime.now());
        log.info("Marked {} notifications as read for userId={}", count, userId);
        return count;
    }

    // ── Delete notification ─────────────────────────────────────────────────────

    @Transactional
    public void deleteNotification(String id) {
        if (!notificationRepository.existsById(id)) {
            throw ApiException.notFound("Notification", id);
        }
        notificationRepository.deleteById(id);
        log.info("Notification deleted: id={}", id);
    }

    // ── Send notification (programmatic) ────────────────────────────────────────

    @Transactional
    public NotificationDTO sendNotification(String userId, String title, String message,
                                             NotificationType type, NotificationChannel channel,
                                             NotificationPriority priority, String link) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .channel(channel != null ? channel : NotificationChannel.IN_APP)
                .priority(priority != null ? priority : NotificationPriority.NORMAL)
                .link(link)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification sent: id={}, userId={}, type={}, channel={}", saved.getId(), userId, type, channel);
        return toDTO(saved);
    }

    // ── Mapper ──────────────────────────────────────────────────────────────────

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .channel(n.getChannel())
                .priority(n.getPriority())
                .isRead(n.getIsRead())
                .readAt(n.getReadAt())
                .link(n.getLink())
                .metadata(n.getMetadata())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
