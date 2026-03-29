package com.commerce_pro_backend.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

/**
 * Batch stock transfer request — allows transferring multiple products
 * between the same source and destination warehouse in one transaction.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BatchStockTransferRequestDTO {

    @NotBlank(message = "From warehouse ID is required")
    private String fromWarehouseId;

    @NotBlank(message = "To warehouse ID is required")
    private String toWarehouseId;

    @NotEmpty(message = "At least one transfer line is required")
    @Valid
    private List<TransferLineDTO> lines;

    private String notes;
    private String reference;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TransferLineDTO {
        @NotBlank(message = "Product ID is required")
        private String productId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        private String notes;
    }
}
