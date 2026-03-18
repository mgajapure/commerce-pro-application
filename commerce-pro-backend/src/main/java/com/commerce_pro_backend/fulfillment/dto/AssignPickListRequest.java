package com.commerce_pro_backend.fulfillment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class AssignPickListRequest {

    @NotBlank(message = "Assignee user ID is required")
    private String userId;

    private String userName;
}
