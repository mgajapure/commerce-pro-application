package com.commerce_pro_backend.notification.dto;

import com.commerce_pro_backend.notification.enums.NotificationChannel;
import com.commerce_pro_backend.notification.enums.NotificationPriority;
import com.commerce_pro_backend.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    private String id;
    private String userId;
    private String title;
    private String message;
    private NotificationType type;
    private NotificationChannel channel;
    private NotificationPriority priority;
    private Boolean isRead;
    private LocalDateTime readAt;
    private String link;
    private String metadata;
    private LocalDateTime createdAt;
}
