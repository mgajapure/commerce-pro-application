package com.commerce_pro_backend.notification.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.notification.dto.CreateNotificationRequest;
import com.commerce_pro_backend.notification.dto.NotificationDTO;
import com.commerce_pro_backend.notification.dto.NotificationStatsDTO;
import com.commerce_pro_backend.notification.service.NotificationService;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @Operation(summary = "Get notifications for current user",
               description = "Returns paginated list of notifications for the authenticated user.")
    public ResponseEntity<ApiResponse<PageResponse<NotificationDTO>>> getNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = currentUserService.getCurrentUserId();
        Page<NotificationDTO> page = notificationService.getNotifications(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", PageResponse.from(page)));
    }

    @GetMapping("/unread")
    @Operation(summary = "Get unread notifications",
               description = "Returns all unread notifications for the authenticated user.")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getUnreadNotifications() {
        String userId = currentUserService.getCurrentUserId();
        List<NotificationDTO> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success("Unread notifications retrieved successfully", notifications));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get notification statistics",
               description = "Returns notification statistics for the authenticated user including counts by type and priority.")
    public ResponseEntity<ApiResponse<NotificationStatsDTO>> getStats() {
        String userId = currentUserService.getCurrentUserId();
        NotificationStatsDTO stats = notificationService.getStats(userId);
        return ResponseEntity.ok(ApiResponse.success("Notification stats retrieved successfully", stats));
    }

    @PostMapping
    @Operation(summary = "Create notification",
               description = "Creates a new notification. Intended for admin or system use.")
    public ResponseEntity<ApiResponse<NotificationDTO>> createNotification(
            @Valid @RequestBody CreateNotificationRequest request) {
        NotificationDTO notification = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Notification created successfully", notification));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read",
               description = "Marks a specific notification as read.")
    public ResponseEntity<ApiResponse<NotificationDTO>> markAsRead(@PathVariable String id) {
        NotificationDTO notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", notification));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read",
               description = "Marks all notifications as read for the authenticated user.")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead() {
        String userId = currentUserService.getCurrentUserId();
        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", count));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification",
               description = "Deletes a specific notification by ID.")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable String id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted successfully", null));
    }
}
