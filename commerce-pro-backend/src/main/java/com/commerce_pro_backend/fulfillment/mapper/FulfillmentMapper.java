package com.commerce_pro_backend.fulfillment.mapper;

import com.commerce_pro_backend.fulfillment.dto.*;
import com.commerce_pro_backend.fulfillment.entity.Carrier;
import com.commerce_pro_backend.fulfillment.entity.PickList;
import com.commerce_pro_backend.fulfillment.entity.PickListItem;
import com.commerce_pro_backend.fulfillment.entity.ShippingRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FulfillmentMapper {

    // ── PickList ──────────────────────────────────────────────────────────────

    public PickListSummaryDTO toSummaryDTO(PickList pl) {
        if (pl == null) return null;
        int pending = pl.getTotalItems() - pl.getCompletedItems();
        double pct  = pl.getTotalItems() == 0 ? 0.0
                : (pl.getCompletedItems() * 100.0) / pl.getTotalItems();
        return PickListSummaryDTO.builder()
                .id(pl.getId())
                .pickListNumber(pl.getPickListNumber())
                .type(pl.getType() != null ? pl.getType().name() : null)
                .status(pl.getStatus() != null ? pl.getStatus().name() : null)
                .warehouseName(pl.getWarehouseName())
                .assignedToName(pl.getAssignedToName())
                .totalOrders(pl.getTotalOrders())
                .totalItems(pl.getTotalItems())
                .completedItems(pl.getCompletedItems())
                .completionPercentage(Math.round(pct * 10.0) / 10.0)
                .generatedAt(pl.getGeneratedAt())
                .completedAt(pl.getCompletedAt())
                .createdAt(pl.getCreatedAt())
                .build();
    }

    public List<PickListSummaryDTO> toSummaryDTOList(List<PickList> list) {
        if (list == null) return List.of();
        return list.stream().map(this::toSummaryDTO).collect(Collectors.toList());
    }

    public PickListDTO toDTO(PickList pl) {
        if (pl == null) return null;
        int pending = pl.getTotalItems() - pl.getCompletedItems();
        double pct  = pl.getTotalItems() == 0 ? 0.0
                : (pl.getCompletedItems() * 100.0) / pl.getTotalItems();
        return PickListDTO.builder()
                .id(pl.getId())
                .pickListNumber(pl.getPickListNumber())
                .type(pl.getType() != null ? pl.getType().name() : null)
                .status(pl.getStatus() != null ? pl.getStatus().name() : null)
                .warehouseId(pl.getWarehouseId())
                .warehouseName(pl.getWarehouseName())
                .assignedToUserId(pl.getAssignedToUserId())
                .assignedToName(pl.getAssignedToName())
                .totalOrders(pl.getTotalOrders())
                .totalItems(pl.getTotalItems())
                .completedItems(pl.getCompletedItems())
                .pendingItems(pending)
                .completionPercentage(Math.round(pct * 10.0) / 10.0)
                .notes(pl.getNotes())
                .generatedAt(pl.getGeneratedAt())
                .assignedAt(pl.getAssignedAt())
                .startedAt(pl.getStartedAt())
                .completedAt(pl.getCompletedAt())
                .createdAt(pl.getCreatedAt())
                .updatedAt(pl.getUpdatedAt())
                .createdBy(pl.getCreatedBy())
                .items(toItemDTOList(pl.getItems()))
                .build();
    }

    public PickListItemDTO toItemDTO(PickListItem item) {
        if (item == null) return null;
        return PickListItemDTO.builder()
                .id(item.getId())
                .orderId(item.getOrderId())
                .orderNumber(item.getOrderNumber())
                .orderItemId(item.getOrderItemId())
                .productId(item.getProductId())
                .sku(item.getSku())
                .productName(item.getProductName())
                .variantInfo(item.getVariantInfo())
                .binLocation(item.getBinLocation())
                .quantityRequired(item.getQuantityRequired())
                .quantityPicked(item.getQuantityPicked())
                .isPicked(item.getIsPicked())
                .notes(item.getNotes())
                .createdAt(item.getCreatedAt())
                .build();
    }

    public List<PickListItemDTO> toItemDTOList(List<PickListItem> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toItemDTO).collect(Collectors.toList());
    }

    // ── Carrier ───────────────────────────────────────────────────────────────

    public CarrierDTO toCarrierDTO(Carrier c) {
        if (c == null) return null;
        return CarrierDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .code(c.getCode())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .trackingUrlTemplate(c.getTrackingUrlTemplate())
                .logoUrl(c.getLogoUrl())
                .isDefault(c.getIsDefault())
                .notes(c.getNotes())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    public List<CarrierDTO> toCarrierDTOList(List<Carrier> carriers) {
        if (carriers == null) return List.of();
        return carriers.stream().map(this::toCarrierDTO).collect(Collectors.toList());
    }

    // ── ShippingRule ──────────────────────────────────────────────────────────

    public ShippingRuleDTO toRuleDTO(ShippingRule r) {
        if (r == null) return null;
        return ShippingRuleDTO.builder()
                .id(r.getId())
                .name(r.getName())
                .description(r.getDescription())
                .carrierId(r.getCarrierId())
                .carrierName(r.getCarrierName())
                .conditionType(r.getConditionType())
                .conditionMin(r.getConditionMin())
                .conditionMax(r.getConditionMax())
                .conditionValue(r.getConditionValue())
                .shippingMethod(r.getShippingMethod())
                .serviceLevel(r.getServiceLevel())
                .priority(r.getPriority())
                .isActive(r.getIsActive())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    public List<ShippingRuleDTO> toRuleDTOList(List<ShippingRule> rules) {
        if (rules == null) return List.of();
        return rules.stream().map(this::toRuleDTO).collect(Collectors.toList());
    }
}
