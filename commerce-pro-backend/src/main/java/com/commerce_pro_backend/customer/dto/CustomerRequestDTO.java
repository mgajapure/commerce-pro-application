package com.commerce_pro_backend.customer.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerRequestDTO {

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255)
    private String email;

    @Size(max = 30)
    private String phone;

    @Size(max = 30)
    private String secondaryPhone;

    private LocalDate dateOfBirth;

    @Size(max = 20)
    private String gender;

    private String avatarUrl;

    @Size(max = 100)
    private String companyName;

    @Size(max = 50)
    private String taxId;

    @Size(max = 3)
    private String preferredCurrency;

    @Size(max = 10)
    private String preferredLanguage;

    private Boolean marketingOptIn;
    private Boolean smsOptIn;

    @Size(max = 100)
    private String acquisitionSource;

    /** Optional referral code used during registration */
    @Size(max = 50)
    private String referralCode;

    /** ID of the customer who referred this customer (B2B referral chain) */
    private String referredByCustomerId;

    private String groupId;
    private String internalNotes;
    private String linkedUserId;
}
