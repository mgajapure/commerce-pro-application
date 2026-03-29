package com.commerce_pro_backend.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRequestDTO {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Warehouse ID is required")
    private String warehouseId;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @Min(value = 0, message = "Reserved quantity cannot be negative")
    private Integer reserved;

    @Min(value = 0, message = "Low stock threshold cannot be negative")
    private Integer lowStockThreshold;

    @Min(value = 0, message = "Reorder point cannot be negative")
    private Integer reorderPoint;

    @Min(value = 0, message = "Reorder quantity cannot be negative")
    private Integer reorderQuantity;

    @Min(value = 0, message = "Max stock level cannot be negative")
    private Integer maxStockLevel;

    @Min(value = 0, message = "Safety stock cannot be negative")
    private Integer safetyStock;

    @DecimalMin(value = "0", message = "Unit cost cannot be negative")
    private BigDecimal unitCost;

    private String binLocation;
    private String aisle;
    private String zone;
    private Boolean trackInventory;
}
