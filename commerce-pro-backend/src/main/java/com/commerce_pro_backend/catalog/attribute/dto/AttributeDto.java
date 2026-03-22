package com.commerce_pro_backend.catalog.attribute.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

public class AttributeDto {

    @Data
    @Builder
    public static class Request {
        @NotBlank
        @Size(max = 100)
        private String name;

        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[a-z0-9_]+$", message = "Code must be lowercase alphanumeric with underscores")
        private String code;

        @NotBlank
        private String type;

        @Size(max = 500)
        private String description;

        private Boolean isRequired;
        private Boolean isFilterable;
        private Boolean isSearchable;
        private Boolean isActive;
        private Integer sortOrder;

        @Valid
        private List<OptionRequest> options;
    }

    @Data
    @Builder
    public static class OptionRequest {
        @NotBlank
        @Size(max = 200)
        private String value;

        @Size(max = 200)
        private String label;

        private Integer sortOrder;

        @Size(max = 7)
        private String colorHex;
    }

    @Data
    @Builder
    public static class Response {
        private String id;
        private String name;
        private String code;
        private String type;
        private String description;
        private Boolean isRequired;
        private Boolean isFilterable;
        private Boolean isSearchable;
        private Boolean isActive;
        private Integer sortOrder;
        private List<OptionResponse> options;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    public static class OptionResponse {
        private String id;
        private String value;
        private String label;
        private Integer sortOrder;
        private String colorHex;
    }

    @Data
    @Builder
    public static class ListResponse {
        private String id;
        private String name;
        private String code;
        private String type;
        private Boolean isRequired;
        private Boolean isFilterable;
        private Boolean isSearchable;
        private Boolean isActive;
        private Integer sortOrder;
        private int optionCount;
    }
}
