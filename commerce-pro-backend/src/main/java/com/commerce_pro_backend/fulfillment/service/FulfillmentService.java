package com.commerce_pro_backend.fulfillment.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.fulfillment.dto.*;
import com.commerce_pro_backend.fulfillment.entity.*;
import com.commerce_pro_backend.fulfillment.enums.*;
import com.commerce_pro_backend.fulfillment.mapper.FulfillmentMapper;
import com.commerce_pro_backend.fulfillment.repository.*;
import com.commerce_pro_backend.fulfillment.specification.PickListSpecification;
import com.commerce_pro_backend.inventory.entity.Inventory;
import com.commerce_pro_backend.inventory.repository.InventoryRepository;
import com.commerce_pro_backend.order.entity.Order;
import com.commerce_pro_backend.order.entity.OrderItem;
import com.commerce_pro_backend.order.enums.FulfillmentStatus;
import com.commerce_pro_backend.order.enums.OrderStatus;
import com.commerce_pro_backend.order.repository.OrderRepository;
import com.commerce_pro_backend.order.repository.OrderItemRepository;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.service.AuditService;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FulfillmentService {

    private final PickListRepository          pickListRepository;
    private final PickListItemRepository      pickListItemRepository;
    private final PickListNumberService       pickListNumberService;
    private final OrderRepository             orderRepository;
    private final OrderItemRepository         orderItemRepository;
    private final InventoryRepository         inventoryRepository;
    private final CarrierRepository           carrierRepository;
    private final ShippingRuleRepository      shippingRuleRepository;
    private final FulfillmentMapper           fulfillmentMapper;
    private final AuditService                auditService;
    private final CurrentUserService          currentUserService;

    // ══════════════════════════════════════════════════════════════════════════
    // FULFILLMENT QUEUE
    // ══════════════════════════════════════════════════════════════════════════

    /** Orders that are confirmed/processing but not yet fully fulfilled. */
    public List<FulfillmentQueueItemDTO> getQueue() {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> (o.getStatus() == OrderStatus.CONFIRMED
                           || o.getStatus() == OrderStatus.PROCESSING)
                          && (o.getFulfillmentStatus() == FulfillmentStatus.UNFULFILLED
                           || o.getFulfillmentStatus() == FulfillmentStatus.PARTIALLY_FULFILLED))
                .toList();
        return orders.stream().map(this::toQueueItem).collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STATS
    // ══════════════════════════════════════════════════════════════════════════

    public FulfillmentStatsDTO getStats() {
        List<Object[]> plGroups = pickListRepository.countGroupedByStatus();
        Map<String, Long> plBreakdown = plGroups.stream().collect(Collectors.toMap(
                arr -> ((PickListStatus) arr[0]).name(), arr -> (Long) arr[1]));

        return FulfillmentStatsDTO.builder()
                .queueSize(getQueue().size())
                .generatedPickLists(pickListRepository.countByStatus(PickListStatus.GENERATED))
                .inProgressPickLists(pickListRepository.countByStatus(PickListStatus.IN_PROGRESS))
                .completedPickListsToday(0L) // updated by shipment stats
                .pickListStatusBreakdown(plBreakdown)
                .shipmentStatusBreakdown(Map.of())
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PICK LIST — READ
    // ══════════════════════════════════════════════════════════════════════════

    public PageResponse<PickListSummaryDTO> listPickLists(PickListFilterDTO filter, Pageable pageable) {
        Specification<PickList> spec = PickListSpecification.withFilters(
                filter.getSearch(), filter.getStatus(), filter.getType(),
                filter.getAssignedToUserId(), filter.getWarehouseId(),
                filter.getCreatedFrom(), filter.getCreatedTo());
        Page<PickList> page = pickListRepository.findAll(spec, pageable);
        return PageResponse.from(page, fulfillmentMapper.toSummaryDTOList(page.getContent()));
    }

    public PickListDTO getPickList(String id) {
        return fulfillmentMapper.toDTO(findPickList(id));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PICK LIST — GENERATE
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public PickListDTO generatePickList(CreatePickListRequest request) {
        String actorId = currentUserService.getCurrentUserId();

        if (request.getOrderIds() == null || request.getOrderIds().isEmpty()) {
            throw ApiException.badRequest("At least one order ID is required");
        }

        PickListType type = PickListType.BATCH;
        if (request.getType() != null) {
            try { type = PickListType.valueOf(request.getType()); }
            catch (IllegalArgumentException e) { throw ApiException.badRequest("Invalid pick list type: " + request.getType()); }
        }
        if (request.getOrderIds().size() == 1) type = PickListType.SINGLE;

        PickList pickList = PickList.builder()
                .pickListNumber(pickListNumberService.generate())
                .type(type)
                .status(PickListStatus.GENERATED)
                .warehouseId(request.getWarehouseId())
                .warehouseName(request.getWarehouseName())
                .totalOrders(request.getOrderIds().size())
                .notes(request.getNotes())
                .generatedAt(LocalDateTime.now())
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        List<PickListItem> items = buildPickListItems(request.getOrderIds(), pickList);
        pickList.getItems().addAll(items);
        pickList.setTotalItems(items.size());
        pickList.setCompletedItems(0);

        PickList saved = pickListRepository.save(pickList);
        auditService.log(actorId, AuditAction.FULFILLMENT_PICKLIST_CREATED,
                "PICK_LIST", saved.getId(), saved.getPickListNumber(), null, null, true);

        log.info("Generated pick list {} for {} orders", saved.getPickListNumber(), request.getOrderIds().size());
        return fulfillmentMapper.toDTO(saved);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PICK LIST — STATE TRANSITIONS
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public PickListDTO assignPickList(String id, AssignPickListRequest request) {
        String actorId = currentUserService.getCurrentUserId();
        PickList pl = findPickList(id);

        if (pl.getStatus() != PickListStatus.GENERATED) {
            throw ApiException.badRequest("Only GENERATED pick lists can be assigned. Current status: " + pl.getStatus());
        }

        pl.setAssignedToUserId(request.getUserId());
        pl.setAssignedToName(request.getUserName());
        pl.setStatus(PickListStatus.ASSIGNED);
        pl.setAssignedAt(LocalDateTime.now());
        pl.setUpdatedBy(actorId);

        PickList saved = pickListRepository.save(pl);
        auditService.log(actorId, AuditAction.FULFILLMENT_PICKLIST_ASSIGNED,
                "PICK_LIST", saved.getId(), saved.getPickListNumber(),
                PickListStatus.GENERATED.name(), PickListStatus.ASSIGNED.name(), true);
        return fulfillmentMapper.toDTO(saved);
    }

    @Transactional
    public PickListDTO startPickList(String id) {
        String actorId = currentUserService.getCurrentUserId();
        PickList pl = findPickList(id);

        if (pl.getStatus() != PickListStatus.ASSIGNED && pl.getStatus() != PickListStatus.GENERATED) {
            throw ApiException.badRequest("Pick list must be GENERATED or ASSIGNED to start. Current: " + pl.getStatus());
        }

        pl.setStatus(PickListStatus.IN_PROGRESS);
        pl.setStartedAt(LocalDateTime.now());
        pl.setUpdatedBy(actorId);

        PickList saved = pickListRepository.save(pl);
        auditService.log(actorId, AuditAction.FULFILLMENT_PICKLIST_STARTED,
                "PICK_LIST", saved.getId(), saved.getPickListNumber(), null, PickListStatus.IN_PROGRESS.name(), true);
        return fulfillmentMapper.toDTO(saved);
    }

    @Transactional
    public PickListItemDTO updatePickItem(String pickListId, String itemId, UpdatePickItemRequest request) {
        String actorId = currentUserService.getCurrentUserId();
        PickList pl = findPickList(pickListId);

        if (pl.getStatus() == PickListStatus.COMPLETED || pl.getStatus() == PickListStatus.CANCELLED) {
            throw ApiException.badRequest("Cannot update items on a " + pl.getStatus() + " pick list");
        }

        PickListItem item = pl.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("PickListItem", itemId));

        item.markPicked(request.getQuantityPicked());
        if (request.getNotes() != null) item.setNotes(request.getNotes());

        pl.refreshCounts();
        pl.setUpdatedBy(actorId);
        pickListRepository.save(pl);

        return fulfillmentMapper.toItemDTO(item);
    }

    @Transactional
    public PickListDTO completePickList(String id) {
        String actorId = currentUserService.getCurrentUserId();
        PickList pl = findPickList(id);

        if (pl.getStatus() != PickListStatus.IN_PROGRESS && pl.getStatus() != PickListStatus.ASSIGNED) {
            throw ApiException.badRequest("Pick list must be IN_PROGRESS or ASSIGNED to complete. Current: " + pl.getStatus());
        }

        pl.refreshCounts();
        pl.setStatus(PickListStatus.COMPLETED);
        pl.setCompletedAt(LocalDateTime.now());
        pl.setUpdatedBy(actorId);

        // Update OrderItem.fulfilledQuantity and inventory for each item
        for (PickListItem item : pl.getItems()) {
            if (item.getQuantityPicked() > 0) {
                updateOrderItemFulfilled(item.getOrderItemId(), item.getQuantityPicked(), actorId);
                deductInventory(item.getProductId(), item.getQuantityPicked());
            }
        }

        // Recalculate fulfillment status on referenced orders
        collectOrderIds(pl).forEach(this::recalculateOrderFulfillmentStatus);

        PickList saved = pickListRepository.save(pl);
        auditService.log(actorId, AuditAction.FULFILLMENT_PICKLIST_COMPLETED,
                "PICK_LIST", saved.getId(), saved.getPickListNumber(),
                PickListStatus.IN_PROGRESS.name(), PickListStatus.COMPLETED.name(), true);
        log.info("Completed pick list {}", saved.getPickListNumber());
        return fulfillmentMapper.toDTO(saved);
    }

    @Transactional
    public PickListDTO cancelPickList(String id) {
        String actorId = currentUserService.getCurrentUserId();
        PickList pl = findPickList(id);

        if (!pl.isCancellable()) {
            throw ApiException.badRequest("Cannot cancel a pick list in status: " + pl.getStatus());
        }

        pl.setStatus(PickListStatus.CANCELLED);
        pl.setUpdatedBy(actorId);
        PickList saved = pickListRepository.save(pl);
        auditService.log(actorId, AuditAction.FULFILLMENT_PICKLIST_CANCELLED,
                "PICK_LIST", saved.getId(), saved.getPickListNumber(),
                null, PickListStatus.CANCELLED.name(), true);
        return fulfillmentMapper.toDTO(saved);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CARRIERS
    // ══════════════════════════════════════════════════════════════════════════

    public List<CarrierDTO> listCarriers() {
        return fulfillmentMapper.toCarrierDTOList(carrierRepository.findAll());
    }

    @Transactional
    public CarrierDTO createCarrier(CarrierRequestDTO request) {
        String actorId = currentUserService.getCurrentUserId();
        if (carrierRepository.existsByCode(request.getCode())) {
            throw ApiException.conflict("Carrier with code '" + request.getCode() + "' already exists");
        }
        // If new default, clear existing default
        if (Boolean.TRUE.equals(request.getIsDefault())) clearExistingDefault();

        Carrier carrier = Carrier.builder()
                .name(request.getName())
                .code(request.getCode().toUpperCase())
                .trackingUrlTemplate(request.getTrackingUrlTemplate())
                .logoUrl(request.getLogoUrl())
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .notes(request.getNotes())
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        Carrier saved = carrierRepository.save(carrier);
        auditService.log(actorId, AuditAction.FULFILLMENT_CARRIER_CREATED,
                "CARRIER", saved.getId(), saved.getCode(), null, null, true);
        return fulfillmentMapper.toCarrierDTO(saved);
    }

    @Transactional
    public CarrierDTO updateCarrier(String id, CarrierRequestDTO request) {
        String actorId = currentUserService.getCurrentUserId();
        Carrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Carrier", id));

        if (!carrier.getCode().equals(request.getCode()) && carrierRepository.existsByCode(request.getCode())) {
            throw ApiException.conflict("Carrier code '" + request.getCode() + "' already in use");
        }
        if (Boolean.TRUE.equals(request.getIsDefault()) && !carrier.getIsDefault()) clearExistingDefault();

        carrier.setName(request.getName());
        carrier.setCode(request.getCode().toUpperCase());
        carrier.setTrackingUrlTemplate(request.getTrackingUrlTemplate());
        carrier.setLogoUrl(request.getLogoUrl());
        carrier.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
        carrier.setNotes(request.getNotes());
        carrier.setUpdatedBy(actorId);

        Carrier saved = carrierRepository.save(carrier);
        auditService.log(actorId, AuditAction.FULFILLMENT_CARRIER_UPDATED,
                "CARRIER", saved.getId(), saved.getCode(), null, null, true);
        return fulfillmentMapper.toCarrierDTO(saved);
    }

    @Transactional
    public void deleteCarrier(String id) {
        String actorId = currentUserService.getCurrentUserId();
        Carrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Carrier", id));
        carrierRepository.delete(carrier);
        auditService.log(actorId, AuditAction.FULFILLMENT_CARRIER_DELETED,
                "CARRIER", id, carrier.getCode(), null, null, true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SHIPPING RULES
    // ══════════════════════════════════════════════════════════════════════════

    public List<ShippingRuleDTO> listRules() {
        return fulfillmentMapper.toRuleDTOList(shippingRuleRepository.findAll());
    }

    @Transactional
    public ShippingRuleDTO createRule(ShippingRuleRequestDTO request) {
        String actorId = currentUserService.getCurrentUserId();
        String carrierName = resolveCarrierName(request.getCarrierId());

        ShippingRule rule = ShippingRule.builder()
                .name(request.getName())
                .description(request.getDescription())
                .carrierId(request.getCarrierId())
                .carrierName(carrierName)
                .conditionType(request.getConditionType())
                .conditionMin(request.getConditionMin())
                .conditionMax(request.getConditionMax())
                .conditionValue(request.getConditionValue())
                .shippingMethod(request.getShippingMethod())
                .serviceLevel(request.getServiceLevel())
                .priority(request.getPriority() != null ? request.getPriority() : 100)
                .isActive(request.getIsActive() == null || request.getIsActive())
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        ShippingRule saved = shippingRuleRepository.save(rule);
        auditService.log(actorId, AuditAction.FULFILLMENT_RULE_CREATED,
                "SHIPPING_RULE", saved.getId(), saved.getName(), null, null, true);
        return fulfillmentMapper.toRuleDTO(saved);
    }

    @Transactional
    public ShippingRuleDTO updateRule(String id, ShippingRuleRequestDTO request) {
        String actorId = currentUserService.getCurrentUserId();
        ShippingRule rule = shippingRuleRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ShippingRule", id));

        String carrierName = resolveCarrierName(request.getCarrierId());
        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setCarrierId(request.getCarrierId());
        rule.setCarrierName(carrierName);
        rule.setConditionType(request.getConditionType());
        rule.setConditionMin(request.getConditionMin());
        rule.setConditionMax(request.getConditionMax());
        rule.setConditionValue(request.getConditionValue());
        rule.setShippingMethod(request.getShippingMethod());
        rule.setServiceLevel(request.getServiceLevel());
        if (request.getPriority() != null) rule.setPriority(request.getPriority());
        if (request.getIsActive() != null) rule.setIsActive(request.getIsActive());
        rule.setUpdatedBy(actorId);

        ShippingRule saved = shippingRuleRepository.save(rule);
        auditService.log(actorId, AuditAction.FULFILLMENT_RULE_UPDATED,
                "SHIPPING_RULE", saved.getId(), saved.getName(), null, null, true);
        return fulfillmentMapper.toRuleDTO(saved);
    }

    @Transactional
    public void deleteRule(String id) {
        String actorId = currentUserService.getCurrentUserId();
        ShippingRule rule = shippingRuleRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ShippingRule", id));
        shippingRuleRepository.delete(rule);
        auditService.log(actorId, AuditAction.FULFILLMENT_RULE_DELETED,
                "SHIPPING_RULE", id, rule.getName(), null, null, true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private PickList findPickList(String id) {
        return pickListRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("PickList", id));
    }

    private List<PickListItem> buildPickListItems(List<String> orderIds, PickList pickList) {
        List<PickListItem> items = new ArrayList<>();
        for (String orderId : orderIds) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> ApiException.notFound("Order", orderId));

            if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.PROCESSING) {
                throw ApiException.badRequest("Order " + order.getOrderNumber()
                        + " must be CONFIRMED or PROCESSING to be picked. Current: " + order.getStatus());
            }

            for (var orderItem : order.getItems()) {
                int pending = orderItem.getPendingFulfillmentQuantity();
                if (pending <= 0) continue;

                items.add(PickListItem.builder()
                        .pickList(pickList)
                        .orderId(order.getId())
                        .orderNumber(order.getOrderNumber())
                        .orderItemId(orderItem.getId())
                        .productId(orderItem.getProductId())
                        .sku(orderItem.getSku())
                        .productName(orderItem.getProductName())
                        .variantInfo(orderItem.getVariantInfo())
                        .quantityRequired(pending)
                        .quantityPicked(0)
                        .isPicked(false)
                        .build());
            }
        }
        if (items.isEmpty()) {
            throw ApiException.badRequest("No pending items found for the selected orders");
        }
        return items;
    }

    private void updateOrderItemFulfilled(String orderItemId, int qty, String actorId) {
        orderItemRepository.findById(orderItemId).ifPresent(item -> {
            item.setFulfilledQuantity(item.getFulfilledQuantity() + qty);
            orderItemRepository.save(item);
        });
    }

    private void deductInventory(String productId, int qty) {
        if (productId == null) return;
        List<Inventory> records = inventoryRepository.findByProductId(productId);
        int remaining = qty;
        for (Inventory inv : records) {
            if (remaining <= 0) break;
            int currentQty = inv.getQuantity();
            int toDeduct = Math.min(remaining, currentQty);
            if (toDeduct > 0) {
                inv.adjustStock(currentQty - toDeduct,"FULFILLMENT", "Deducted on pick list completion", null);
                inventoryRepository.save(inv);
                remaining -= toDeduct;
            }
        }
        if (remaining > 0) {
            log.warn("Inventory shortfall when deducting for product {}: {} units unaccounted", productId, remaining);
        }
    }

    private void recalculateOrderFulfillmentStatus(String orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            boolean allFulfilled = order.getItems().stream()
                    .allMatch(i -> i.getPendingFulfillmentQuantity() == 0);
            boolean anyFulfilled = order.getItems().stream()
                    .anyMatch(i -> i.getFulfilledQuantity() > 0);

            if (allFulfilled) {
                order.setFulfillmentStatus(FulfillmentStatus.FULFILLED);
                if (order.getStatus() == OrderStatus.PROCESSING) {
                    order.transitionTo(OrderStatus.FULFILLED, "system", "All items fulfilled");
                }
            } else if (anyFulfilled) {
                order.setFulfillmentStatus(FulfillmentStatus.PARTIALLY_FULFILLED);
                order.transitionTo(OrderStatus.PARTIALLY_FULFILLED, "system", "Some items fulfilled");
            }
            orderRepository.save(order);
        });
    }

    private List<String> collectOrderIds(PickList pl) {
        return pl.getItems().stream()
                .map(PickListItem::getOrderId)
                .distinct()
                .collect(Collectors.toList());
    }

    private void clearExistingDefault() {
        carrierRepository.findByIsDefaultTrue().ifPresent(c -> {
            c.setIsDefault(false);
            carrierRepository.save(c);
        });
    }

    private String resolveCarrierName(String carrierId) {
        if (carrierId == null) return null;
        return carrierRepository.findById(carrierId).map(Carrier::getName).orElse(null);
    }

    private FulfillmentQueueItemDTO toQueueItem(Order o) {
        int totalItems   = o.getItems().size();
        int pendingItems = o.getItems().stream().mapToInt(i -> i.getPendingFulfillmentQuantity()).sum();
        return FulfillmentQueueItemDTO.builder()
                .orderId(o.getId())
                .orderNumber(o.getOrderNumber())
                .customerName(o.getCustomerName())
                .customerEmail(o.getCustomerEmail())
                .status(o.getStatus().name())
                .fulfillmentStatus(o.getFulfillmentStatus().name())
                .totalAmount(o.getTotalAmount())
                .currency(o.getCurrency())
                .totalItems(totalItems)
                .pendingItems(pendingItems)
                .confirmedAt(o.getConfirmedAt())
                .createdAt(o.getCreatedAt())
                .isFlagged(o.getIsFlagged())
                .riskScore(o.getRiskScore())
                .shippingMethod(o.getShippingMethod())
                .build();
    }
}
