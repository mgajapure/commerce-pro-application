package com.commerce_pro_backend.customer.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class SavedCartDTO {
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        @Size(max = 100) private String cartName;
        private String couponCode;
        private List<ItemRequest> items;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ItemRequest {
        @NotBlank private String productId;
        @NotNull  private Integer quantity;
        private String variantInfo;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private String id;
        private String cartName;
        private String couponCode;
        private BigDecimal estimatedTotal;
        private Boolean isAbandoned;
        private LocalDateTime abandonedAt;
        private Boolean recoveryEmailSent;
        private List<ItemResponse> items;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ItemResponse {
        private String id;
        private String productId;
        private String productName;
        private String productSku;
        private String productImageUrl;
        private BigDecimal unitPrice;
        private Integer quantity;
        private String variantInfo;
        private LocalDateTime addedAt;
    }
}
