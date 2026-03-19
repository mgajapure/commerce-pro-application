package com.commerce_pro_backend.customer.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

public class AddressDTO {
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        @Size(max = 50)  private String addressLabel;
        private String addressType;
        @NotBlank @Size(max = 200) private String fullName;
        @Size(max = 30)  private String phone;
        @Size(max = 100) private String company;
        @NotBlank @Size(max = 300) private String addressLine1;
        @Size(max = 300) private String addressLine2;
        @NotBlank @Size(max = 100) private String city;
        @Size(max = 100) private String state;
        @NotBlank @Size(max = 20)  private String postalCode;
        @NotBlank @Size(max = 100) private String country;
        private Boolean isDefault;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private String id;
        private String addressLabel;
        private String addressType;
        private String fullName;
        private String phone;
        private String company;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private Boolean isDefault;
        private Boolean isVerified;
        private LocalDateTime createdAt;
    }
}
