package com.commerce_pro_backend.supplier.directory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

public class SupplierDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank
        @Size(max = 100)
        private String name;

        @NotBlank
        @Size(max = 20)
        private String code;

        @Size(max = 100)
        private String contactPerson;

        @Email
        @Size(max = 255)
        private String email;

        @Size(max = 50)
        private String phone;

        @Size(max = 500)
        private String address;

        @Size(max = 100)
        private String city;

        @Size(max = 100)
        private String state;

        @Size(max = 100)
        private String country;

        @Size(max = 20)
        private String postalCode;

        @Size(max = 500)
        private String website;

        @Size(max = 2000)
        private String description;

        @Size(max = 100)
        private String paymentTerms;

        private Integer paymentTermsDays;

        private Integer leadTimeDays;

        private BigDecimal minOrderValue;

        @Size(max = 10)
        private String preferredCurrency;

        @Size(max = 1000)
        private String certifications;

        @Size(max = 2000)
        private String notes;

        private Boolean isActive;

        private Boolean isPreferred;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String name;
        private String code;
        private String contactPerson;
        private String email;
        private String phone;
        private String address;
        private String city;
        private String state;
        private String country;
        private String postalCode;
        private String website;
        private String description;
        private String paymentTerms;
        private Integer paymentTermsDays;
        private Integer leadTimeDays;
        private Double rating;
        private BigDecimal minOrderValue;
        private String preferredCurrency;
        private String certifications;
        private String notes;
        private Boolean isActive;
        private Boolean isPreferred;
        private Integer productCount;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private String id;
        private String name;
        private String code;
        private String contactPerson;
        private String email;
        private String phone;
        private Double rating;
        private Boolean isActive;
        private Boolean isPreferred;
        private Integer productCount;
    }
}
