package com.commerce_pro_backend.customer.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GroupDTO {
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        @NotBlank @Size(max = 100) private String name;
        @Size(max = 500) private String description;
        @Size(max = 7)   private String colorHex;
        private Boolean isActive;
        private BigDecimal discountPercentage;
        private String internalNotes;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private String id;
        private String name;
        private String description;
        private String colorHex;
        private Boolean isActive;
        private BigDecimal discountPercentage;
        private String internalNotes;
        private Integer memberCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
