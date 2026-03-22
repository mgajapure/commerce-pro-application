package com.commerce_pro_backend.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatsDTO {

    private long total;
    private long unread;
    private Map<String, Integer> byType;
    private Map<String, Integer> byPriority;
}
