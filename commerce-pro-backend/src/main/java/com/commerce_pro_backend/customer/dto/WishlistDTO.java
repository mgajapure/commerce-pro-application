package com.commerce_pro_backend.customer.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class WishlistDTO {
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        @NotBlank @Size(max = 100) private String name;
        private String visibility;
        private Boolean isDefault;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AddItemRequest {
        @NotBlank private String productId;
        private String variantInfo;
        private String note;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private String id;
        private String name;
        private String visibility;
        private String shareToken;
        private Boolean isDefault;
        private Integer itemCount;
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
        private BigDecimal priceAtAdd;
        private String variantInfo;
        private String note;
        private LocalDateTime addedAt;
    }
}
