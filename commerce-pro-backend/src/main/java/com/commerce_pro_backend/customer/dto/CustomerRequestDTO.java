package com.commerce_pro_backend.customer.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerRequestDTO {
    @NotBlank @Size(max = 100) private String firstName;
    @NotBlank @Size(max = 100) private String lastName;
    @NotBlank @Email @Size(max = 255) private String email;
    @Size(max = 30)  private String phone;
    @Size(max = 30)  private String secondaryPhone;
    private LocalDate dateOfBirth;
    @Size(max = 20)  private String gender;
    private String avatarUrl;
    @Size(max = 100) private String companyName;
    @Size(max = 50)  private String taxId;
    private String preferredCurrency;
    private String preferredLanguage;
    private Boolean marketingOptIn;
    private Boolean smsOptIn;
    @Size(max = 100) private String acquisitionSource;
    private String groupId;
    private String internalNotes;
    private String linkedUserId;
}
