package com.commerce_pro_backend.fulfillment.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CarrierRequestDTO {

    @NotBlank(message = "Carrier name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Carrier code is required")
    @Size(max = 20)
    private String code;

    @Size(max = 500)
    private String trackingUrlTemplate;

    private String logoUrl;

    private Boolean isDefault;

    @Size(max = 1000)
    private String notes;
}
