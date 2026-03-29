package com.commerce_pro_backend.fulfillment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor
public class AddTrackingEventRequest {

    @NotBlank(message = "Status is required")
    @Size(max = 30, message = "Status must be a valid ShipmentStatus name")
    private String status;   // ShipmentStatus enum name

    @NotBlank(message = "Description is required")
    @Size(max = 500)
    private String description;

    @Size(max = 200)
    private String location;

    private LocalDateTime eventTime; // if null, defaults to now
}
