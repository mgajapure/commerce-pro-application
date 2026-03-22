package com.commerce_pro_backend.notification.dto;

import com.commerce_pro_backend.notification.enums.NotificationChannel;
import com.commerce_pro_backend.notification.enums.NotificationPriority;
import com.commerce_pro_backend.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    @NotNull(message = "Type is required")
    private NotificationType type;

    @Builder.Default
    private NotificationChannel channel = NotificationChannel.IN_APP;

    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    private String link;

    private String metadata;
}
