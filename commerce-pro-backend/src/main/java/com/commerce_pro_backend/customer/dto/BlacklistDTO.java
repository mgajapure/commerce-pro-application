package com.commerce_pro_backend.customer.dto;

import jakarta.validation.constraints.*;
import lombok.*;

public class BlacklistDTO {
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BlacklistRequest {
        @NotBlank @Size(max = 500) private String reason;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FraudFlagRequest {
        @NotBlank @Size(max = 500) private String reason;
        private String evidence;
        private String relatedOrderId;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FraudResolveRequest {
        @NotBlank @Size(max = 1000) private String resolution;
    }
}
