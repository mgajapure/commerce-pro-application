package com.commerce_pro_backend.customer.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

public class CommunicationLogDTO {
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        @NotBlank private String channel;
        @NotBlank private String direction;
        @NotBlank @Size(max = 200) private String subject;
        private String body;
        private String orderId;
        private String orderNumber;
        private Boolean requiresFollowUp;
        private LocalDateTime followUpAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private String id;
        private String channel;
        private String direction;
        private String subject;
        private String body;
        private String orderId;
        private String orderNumber;
        private String loggedBy;
        private String loggedByName;
        private LocalDateTime loggedAt;
        private Boolean requiresFollowUp;
        private LocalDateTime followUpAt;
        private Boolean isResolved;
        private LocalDateTime createdAt;
    }
}
