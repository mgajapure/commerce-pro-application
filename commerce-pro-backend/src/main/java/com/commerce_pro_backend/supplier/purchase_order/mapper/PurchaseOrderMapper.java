package com.commerce_pro_backend.supplier.purchase_order.mapper;

import com.commerce_pro_backend.supplier.purchase_order.dto.PurchaseOrderDto;
import com.commerce_pro_backend.supplier.purchase_order.entity.PurchaseOrder;
import com.commerce_pro_backend.supplier.purchase_order.entity.PurchaseOrderItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PurchaseOrderMapper {

    public PurchaseOrder toEntity(PurchaseOrderDto.Request dto) {
        if (dto == null) {
            return null;
        }

        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .supplierId(dto.getSupplierId())
                .supplierName(dto.getSupplierName())
                .status(dto.getStatus() != null ? dto.getStatus() : "DRAFT")
                .orderDate(dto.getOrderDate())
                .expectedDeliveryDate(dto.getExpectedDeliveryDate())
                .notes(dto.getNotes())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "USD")
                .items(new ArrayList<>())
                .build();

        if (dto.getItems() != null) {
            for (PurchaseOrderDto.ItemRequest itemDto : dto.getItems()) {
                PurchaseOrderItem item = toItemEntity(itemDto);
                if (item != null) {
                    item.setId(UUID.randomUUID().toString());
                    item.setPurchaseOrder(purchaseOrder);
                    purchaseOrder.getItems().add(item);
                }
            }
        }

        return purchaseOrder;
    }

    public PurchaseOrderItem toItemEntity(PurchaseOrderDto.ItemRequest dto) {
        if (dto == null) {
            return null;
        }

        return PurchaseOrderItem.builder()
                .productId(dto.getProductId())
                .productName(dto.getProductName())
                .sku(dto.getSku())
                .quantityOrdered(dto.getQuantityOrdered())
                .unitCost(dto.getUnitCost())
                .notes(dto.getNotes())
                .build();
    }

    public PurchaseOrderDto.ItemResponse toItemResponse(PurchaseOrderItem entity) {
        if (entity == null) {
            return null;
        }

        return PurchaseOrderDto.ItemResponse.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .productName(entity.getProductName())
                .sku(entity.getSku())
                .quantityOrdered(entity.getQuantityOrdered())
                .quantityReceived(entity.getQuantityReceived())
                .unitCost(entity.getUnitCost())
                .totalCost(entity.getTotalCost())
                .build();
    }

    public PurchaseOrderDto.Response toResponse(PurchaseOrder entity) {
        if (entity == null) {
            return null;
        }

        List<PurchaseOrderDto.ItemResponse> itemResponses = null;
        if (entity.getItems() != null) {
            itemResponses = entity.getItems().stream()
                    .map(this::toItemResponse)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        return PurchaseOrderDto.Response.builder()
                .id(entity.getId())
                .poNumber(entity.getPoNumber())
                .supplierId(entity.getSupplierId())
                .supplierName(entity.getSupplierName())
                .status(entity.getStatus())
                .orderDate(entity.getOrderDate())
                .expectedDeliveryDate(entity.getExpectedDeliveryDate())
                .actualDeliveryDate(entity.getActualDeliveryDate())
                .subtotal(entity.getSubtotal())
                .taxAmount(entity.getTaxAmount())
                .shippingCost(entity.getShippingCost())
                .totalAmount(entity.getTotalAmount())
                .currency(entity.getCurrency())
                .notes(entity.getNotes())
                .createdBy(entity.getCreatedBy())
                .approvedBy(entity.getApprovedBy())
                .approvedAt(entity.getApprovedAt())
                .items(itemResponses)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PurchaseOrderDto.ListResponse toListResponse(PurchaseOrder entity) {
        if (entity == null) {
            return null;
        }

        return PurchaseOrderDto.ListResponse.builder()
                .id(entity.getId())
                .poNumber(entity.getPoNumber())
                .supplierName(entity.getSupplierName())
                .status(entity.getStatus())
                .orderDate(entity.getOrderDate())
                .expectedDeliveryDate(entity.getExpectedDeliveryDate())
                .totalAmount(entity.getTotalAmount())
                .currency(entity.getCurrency())
                .build();
    }

    public List<PurchaseOrderDto.ListResponse> toListResponseList(List<PurchaseOrder> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .map(this::toListResponse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void updateEntityFromDto(PurchaseOrderDto.Request dto, PurchaseOrder entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getSupplierId() != null) {
            entity.setSupplierId(dto.getSupplierId());
        }
        if (dto.getSupplierName() != null) {
            entity.setSupplierName(dto.getSupplierName());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getOrderDate() != null) {
            entity.setOrderDate(dto.getOrderDate());
        }
        if (dto.getExpectedDeliveryDate() != null) {
            entity.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
        }
        if (dto.getNotes() != null) {
            entity.setNotes(dto.getNotes());
        }
        if (dto.getCurrency() != null) {
            entity.setCurrency(dto.getCurrency());
        }
    }
}
