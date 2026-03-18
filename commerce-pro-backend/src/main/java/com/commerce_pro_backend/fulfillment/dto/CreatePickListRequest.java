package com.commerce_pro_backend.fulfillment.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreatePickListRequest {

    @NotEmpty(message = "At least one order ID is required")
    private List<String> orderIds;

    private String type;           // PickListType — defaults to BATCH

    private String warehouseId;
    private String warehouseName;

    @Size(max = 1000)
    private String notes;
}
