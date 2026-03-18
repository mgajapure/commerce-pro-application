package com.commerce_pro_backend.fulfillment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor
public class AddTrackingEventRequest {

    @NotBlank(message = "Status is required")
    private String status;   // ShipmentStatus name

    @NotBlank(message = "Description is required")
    private String description;

    private String location;

    private LocalDateTime eventTime; // if null, defaults to now
}
