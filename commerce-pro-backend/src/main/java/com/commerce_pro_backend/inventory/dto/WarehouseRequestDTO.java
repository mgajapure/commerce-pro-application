package com.commerce_pro_backend.inventory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseRequestDTO {

    @NotBlank(message = "Warehouse name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 20)
    private String code;

    @Size(max = 300)
    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 100)
    private String country;

    @Size(max = 20)
    private String postalCode;

    @Size(max = 200)
    private String managerName;

    @Email(message = "Manager email must be valid")
    @Size(max = 255)
    private String managerEmail;

    @Pattern(regexp = "^[+]?[0-9 \\-().]{7,20}$", message = "Manager phone format is invalid")
    private String managerPhone;

    private Boolean isActive;
    private Boolean isDefault;
}
