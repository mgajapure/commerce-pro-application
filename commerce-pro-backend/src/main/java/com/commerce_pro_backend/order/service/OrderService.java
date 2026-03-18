package com.commerce_pro_backend.order.service;

import com.commerce_pro_backend.catalog.product.entity.Product;
import com.commerce_pro_backend.catalog.product.repository.ProductRepository;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.inventory.entity.Inventory;
import com.commerce_pro_backend.inventory.repository.InventoryRepository;
import com.commerce_pro_backend.order.dto.*;
import com.commerce_pro_backend.order.entity.Order;
import com.commerce_pro_backend.order.entity.OrderItem;
import com.commerce_pro_backend.order.mapper.OrderMapper;
import com.commerce_pro_backend.order.repository.OrderItemRepository;
import com.commerce_pro_backend.order.repository.OrderRepository;
import com.commerce_pro_backend.order.specification.OrderSpecification;
import com.commerce_pro_backend.order.enums.FulfillmentStatus;
import com.commerce_pro_backend.order.enums.OrderSource;
import com.commerce_pro_backend.order.enums.OrderStatus;
import com.commerce_pro_backend.order.enums.PaymentStatus;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core Order Management service.
 *
 * Responsibilities:
 * - Create / update / cancel orders
 * - Status lifecycle transitions with validation
 * - Inventory reservation (reserve on confirm, release on cancel)
 * - Audit logging on every state-changing operation
 * - Dashboard statistics aggregation
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository          orderRepository;
    private final OrderItemRepository      orderItemRepository;
    private final OrderNumberService       orderNumberService;
    private final OrderMapper              orderMapper;
    private final ProductRepository        productRepository;
    private final InventoryRepository      inventoryRepository;
    private final AuditService             auditService;
    private final CurrentUserService       currentUserService;

    // ══════════════════════════════════════════════════════════════════════════
    // READ OPERATIONS
    // ══════════════════════════════════════════════════════════════════════════

    public PageResponse<OrderSummaryDTO> listOrders(OrderFilterDTO filter, Pageable pageable) {
        Specification<Order> spec = OrderSpecification.withFilters(
                filter.getSearch(),
                filter.getStatus(),
                filter.getPaymentStatus(),
                filter.getSource(),
                filter.getCustomerId(),
                filter.getIsFlagged(),
                filter.getCreatedFrom(),
                filter.getCreatedTo()
        );
        Page<Order> page = orderRepository.findAll(spec, pageable);
        return PageResponse.from(page, orderMapper.toSummaryDTOList(page.getContent()));
    }

    public OrderResponseDTO getOrderById(String id) {
        Order order = findOrder(id);
        return orderMapper.toResponseDTO(order);
    }

    public OrderResponseDTO getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> ApiException.notFound("Order", orderNumber));
        return orderMapper.toResponseDTO(order);
    }

    public PageResponse<OrderSummaryDTO> getCustomerOrders(String customerId, Pageable pageable) {
        Page<Order> page = orderRepository.findByCustomerId(customerId, pageable);
        return PageResponse.from(page, orderMapper.toSummaryDTOList(page.getContent()));
    }

    public OrderStatsDTO getStats() {
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();

        List<Object[]> grouped = orderRepository.countGroupedByStatus();
        Map<String, Long> statusBreakdown = grouped.stream()
                .collect(Collectors.toMap(
                        arr -> ((OrderStatus) arr[0]).name(),
                        arr -> (Long) arr[1]
                ));

        return OrderStatsDTO.builder()
                .totalOrders(orderRepository.count())
                .ordersToday(orderRepository.countOrdersSince(startOfToday))
                .pendingOrders(orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT))
                .processingOrders(orderRepository.countByStatus(OrderStatus.PROCESSING))
                .confirmedOrders(orderRepository.countByStatus(OrderStatus.CONFIRMED))
                .shippedOrders(orderRepository.countByStatus(OrderStatus.SHIPPED))
                .deliveredOrders(orderRepository.countByStatus(OrderStatus.DELIVERED))
                .cancelledOrders(orderRepository.countByStatus(OrderStatus.CANCELLED))
                .flaggedOrders(orderRepository.countFlaggedOpen())
                .onHoldOrders(orderRepository.countByStatus(OrderStatus.ON_HOLD))
                .totalRevenue(nullSafe(orderRepository.sumTotalRevenue()))
                .revenueToday(nullSafe(orderRepository.sumRevenueSince(startOfToday)))
                .averageOrderValue(nullSafe(orderRepository.averageOrderValue()))
                .statusBreakdown(statusBreakdown)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CREATE
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public OrderResponseDTO createOrder(CreateOrderRequestDTO request) {
        String actorId = currentUserService.getCurrentUserId();

        // Build the Order shell
        Order order = Order.builder()
                .orderNumber(orderNumberService.generate())
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .source(request.getSource() != null ? request.getSource() : OrderSource.MANUAL)
                .status(OrderStatus.DRAFT)
                .paymentStatus(PaymentStatus.PENDING)
                .fulfillmentStatus(FulfillmentStatus.UNFULFILLED)
                .shippingAddress(orderMapper.toAddressEntity(request.getShippingAddress()))
                .billingAddress(orderMapper.toAddressEntity(request.getBillingAddress()))
                .shippingCost(nullSafe(request.getShippingCost()))
                .discountAmount(nullSafe(request.getDiscountAmount()))
                .couponCode(request.getCouponCode())
                .shippingMethod(request.getShippingMethod())
                .customerNotes(request.getCustomerNotes())
                .internalNotes(request.getInternalNotes())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        // Build and attach line items
        List<OrderItem> items = buildOrderItems(request.getItems(), order);
        order.getItems().addAll(items);
        order.recalculateTotals();

        // Basic fraud / risk scoring heuristic
        order.setRiskScore(calculateRiskScore(order));
        if (order.getRiskScore() >= 70) {
            order.setIsFlagged(true);
        }

        Order saved = orderRepository.save(order);

        auditService.log(actorId, AuditAction.ORDER_CREATED,
                "ORDER", saved.getId(), saved.getOrderNumber(), null, null, true);

        log.info("Created order {} for customer {}", saved.getOrderNumber(), saved.getCustomerEmail());
        return orderMapper.toResponseDTO(saved);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UPDATE  (pre-fulfillment only)
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public OrderResponseDTO updateOrder(String id, UpdateOrderRequestDTO request) {
        String actorId = currentUserService.getCurrentUserId();
        Order order    = findOrder(id);

        if (!order.isEditable()) {
            throw ApiException.badRequest(
                    "Order " + order.getOrderNumber() + " cannot be edited in status " + order.getStatus());
        }

        String oldValue = order.getStatus().name();

        // Update scalar fields if provided
        if (request.getCustomerName()  != null) order.setCustomerName(request.getCustomerName());
        if (request.getCustomerEmail() != null) order.setCustomerEmail(request.getCustomerEmail());
        if (request.getCustomerPhone() != null) order.setCustomerPhone(request.getCustomerPhone());
        if (request.getShippingAddress() != null) order.setShippingAddress(orderMapper.toAddressEntity(request.getShippingAddress()));
        if (request.getBillingAddress()  != null) order.setBillingAddress(orderMapper.toAddressEntity(request.getBillingAddress()));
        if (request.getShippingCost()    != null) order.setShippingCost(request.getShippingCost());
        if (request.getDiscountAmount()  != null) order.setDiscountAmount(request.getDiscountAmount());
        if (request.getCouponCode()      != null) order.setCouponCode(request.getCouponCode());
        if (request.getShippingMethod()  != null) order.setShippingMethod(request.getShippingMethod());
        if (request.getInternalNotes()   != null) order.setInternalNotes(request.getInternalNotes());
        if (request.getCustomerNotes()   != null) order.setCustomerNotes(request.getCustomerNotes());

        // Replace line items if provided
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            order.getItems().clear();
            List<OrderItem> newItems = buildOrderItems(request.getItems(), order);
            order.getItems().addAll(newItems);
        }

        order.recalculateTotals();
        order.setUpdatedBy(actorId);

        Order saved = orderRepository.save(order);

        auditService.log(actorId, AuditAction.ORDER_UPDATED,
                "ORDER", saved.getId(), saved.getOrderNumber(), oldValue, "UPDATED", true);

        return orderMapper.toResponseDTO(saved);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STATUS TRANSITIONS
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public OrderResponseDTO confirmOrder(String id) {
        String actorId = currentUserService.getCurrentUserId();
        Order  order   = findOrder(id);

        validateTransition(order, OrderStatus.CONFIRMED,
                OrderStatus.DRAFT, OrderStatus.PENDING_PAYMENT);

        // Reserve inventory for each line item
        reserveInventory(order);

        order.transitionTo(OrderStatus.CONFIRMED, actorId, "Order confirmed");
        order.setPaymentStatus(PaymentStatus.CAPTURED);
        order.setUpdatedBy(actorId);

        Order saved = orderRepository.save(order);
        auditService.log(actorId, AuditAction.ORDER_CONFIRMED,
                "ORDER", saved.getId(), saved.getOrderNumber(), OrderStatus.PENDING_PAYMENT.name(), OrderStatus.CONFIRMED.name(), true);
        return orderMapper.toResponseDTO(saved);
    }

    @Transactional
    public OrderResponseDTO markProcessing(String id) {
        String actorId = currentUserService.getCurrentUserId();
        Order  order   = findOrder(id);

        validateTransition(order, OrderStatus.PROCESSING, OrderStatus.CONFIRMED, OrderStatus.ON_HOLD);
        order.transitionTo(OrderStatus.PROCESSING, actorId, "Order moved to processing");
        order.setUpdatedBy(actorId);

        Order saved = orderRepository.save(order);
        auditService.log(actorId, AuditAction.ORDER_STATUS_CHANGED,
                "ORDER", saved.getId(), saved.getOrderNumber(), null, OrderStatus.PROCESSING.name(), true);
        return orderMapper.toResponseDTO(saved);
    }

    @Transactional
    public OrderResponseDTO markShipped(String id, TrackingUpdateRequest trackingRequest) {
        String actorId = currentUserService.getCurrentUserId();
        Order  order   = findOrder(id);

        validateTransition(order, OrderStatus.SHIPPED,
                OrderStatus.PROCESSING, OrderStatus.FULFILLED, OrderStatus.PARTIALLY_FULFILLED);

        order.setTrackingNumber(trackingRequest.getTrackingNumber());
        order.setCarrier(trackingRequest.getCarrier());
        order.transitionTo(OrderStatus.SHIPPED, actorId, "Order shipped");
        order.setFulfillmentStatus(FulfillmentStatus.FULFILLED);
        order.setUpdatedBy(actorId);

        Order saved = orderRepository.save(order);
        auditService.log(actorId, AuditAction.ORDER_SHIPPED,
                "ORDER", saved.getId(), saved.getOrderNumber(), null, OrderStatus.SHIPPED.name(), true);
        return orderMapper.toResponseDTO(saved);
    }

    @Transactional
    public OrderResponseDTO markDelivered(String id) {
        String actorId = currentUserService.getCurrentUserId();
        Order  order   = findOrder(id);

        validateTransition(order, OrderStatus.DELIVERED, OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY);
        order.transitionTo(OrderStatus.DELIVERED, actorId, "Order delivered");
        order.setUpdatedBy(actorId);

        Order saved = orderRepository.save(order);
        auditService.log(actorId, AuditAction.ORDER_DELIVERED,
                "ORDER", saved.getId(), saved.getOrderNumber(), null, OrderStatus.DELIVERED.name(), true);
        return orderMapper.toResponseDTO(saved);
    }

    @Transactional
    public OrderResponseDTO holdOrder(String id, OrderHoldRequest request) {
        String actorId = currentUserService.getCurrentUserId();
        Order  order   = findOrder(id);

        if (order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.CLOSED) {
            throw ApiException.badRequest("Cannot hold order in status: " + order.getStatus());
        }

        order.transitionTo(OrderStatus.ON_HOLD, actorId, request.getReason());
        order.setHoldReason(request.getReason());
        order.setUpdatedBy(actorId);

        Order saved = orderRepository.save(order);
        auditService.log(actorId, AuditAction.ORDER_HELD,
                "ORDER", saved.getId(), saved.getOrderNumber(), null, request.getReason(), true);
        return orderMapper.toResponseDTO(saved);
    }

    @Transactional
    public OrderResponseDTO releaseHold(String id) {
        String actorId = currentUserService.getCurrentUserId();
        Order  order   = findOrder(id);

        if (order.getStatus() != OrderStatus.ON_HOLD) {
            throw ApiException.badRequest("Order is not on hold");
        }

        order.transitionTo(OrderStatus.CONFIRMED, actorId, "Hold released");
        order.setHoldReason(null);
        order.setUpdatedBy(actorId);

        Order saved = orderRepository.save(order);
        auditService.log(actorId, AuditAction.ORDER_HOLD_RELEASED,
                "ORDER", saved.getId(), saved.getOrderNumber(), OrderStatus.ON_HOLD.name(), OrderStatus.CONFIRMED.name(), true);
        return orderMapper.toResponseDTO(saved);
    }

    @Transactional
    public OrderResponseDTO cancelOrder(String id, OrderCancelRequest request) {
        String actorId = currentUserService.getCurrentUserId();
        Order  order   = findOrder(id);

        if (!order.isCancellable()) {
            throw ApiException.badRequest(
                    "Order " + order.getOrderNumber() + " cannot be cancelled in status: " + order.getStatus());
        }

        // Release any inventory reservations
        if (order.getStatus() == OrderStatus.CONFIRMED
                || order.getStatus() == OrderStatus.PROCESSING
                || order.getStatus() == OrderStatus.ON_HOLD) {
            releaseInventory(order);
        }

        order.transitionTo(OrderStatus.CANCELLED, actorId, request.getReason());
        order.setCancellationReason(request.getReason());
        order.setUpdatedBy(actorId);

        Order saved = orderRepository.save(order);
        auditService.log(actorId, AuditAction.ORDER_CANCELLED,
                "ORDER", saved.getId(), saved.getOrderNumber(), null, request.getReason(), true);
        return orderMapper.toResponseDTO(saved);
    }

    @Transactional
    public OrderResponseDTO updateTracking(String id, TrackingUpdateRequest request) {
        String actorId = currentUserService.getCurrentUserId();
        Order  order   = findOrder(id);

        order.setTrackingNumber(request.getTrackingNumber());
        order.setCarrier(request.getCarrier());
        order.setUpdatedBy(actorId);

        Order saved = orderRepository.save(order);
        auditService.log(actorId, AuditAction.ORDER_TRACKING_UPDATED,
                "ORDER", saved.getId(), saved.getOrderNumber(), null,
                request.getTrackingNumber(), true);
        return orderMapper.toResponseDTO(saved);
    }

    @Transactional
    public OrderResponseDTO closeOrder(String id) {
        String actorId = currentUserService.getCurrentUserId();
        Order  order   = findOrder(id);

        validateTransition(order, OrderStatus.CLOSED,
                OrderStatus.DELIVERED, OrderStatus.REFUNDED, OrderStatus.PARTIALLY_REFUNDED);

        order.transitionTo(OrderStatus.CLOSED, actorId, "Order closed");
        order.setUpdatedBy(actorId);

        Order saved = orderRepository.save(order);
        auditService.log(actorId, AuditAction.ORDER_CLOSED,
                "ORDER", saved.getId(), saved.getOrderNumber(), null, OrderStatus.CLOSED.name(), true);
        return orderMapper.toResponseDTO(saved);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BULK ACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public List<String> bulkAction(BulkOrderActionRequest request) {
        String actorId = currentUserService.getCurrentUserId();
        List<String> failed = new ArrayList<>();

        for (String orderId : request.getOrderIds()) {
            try {
                Order order = findOrder(orderId);
                OrderCancelRequest cancelReq = new OrderCancelRequest();
                cancelReq.setReason(request.getReason() != null ? request.getReason() : "Bulk action");

                switch (request.getTargetStatus()) {
                    case CANCELLED  -> cancelOrder(orderId, cancelReq);
                    case CONFIRMED  -> confirmOrder(orderId);
                    case PROCESSING -> markProcessing(orderId);
                    case ON_HOLD    -> {
                        OrderHoldRequest holdReq = new OrderHoldRequest();
                        holdReq.setReason(request.getReason() != null ? request.getReason() : "Bulk hold");
                        holdOrder(orderId, holdReq);
                    }
                    default -> failed.add(orderId + ": unsupported bulk target status");
                }
            } catch (Exception e) {
                log.warn("Bulk action failed for order {}: {}", orderId, e.getMessage());
                failed.add(orderId + ": " + e.getMessage());
            }
        }

        auditService.log(actorId, AuditAction.ORDER_BULK_ACTION,
                "ORDER", "BULK", request.getTargetStatus().name(),
                null, "Processed " + request.getOrderIds().size() + " orders", true);

        return failed;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private Order findOrder(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Order", id));
    }

    /**
     * Build OrderItem entities from the request DTOs, resolving product details
     * from the catalogue and snapshotting them onto the item.
     */
    private List<OrderItem> buildOrderItems(List<OrderItemRequestDTO> itemRequests, Order order) {
        List<OrderItem> items = new ArrayList<>();

        for (OrderItemRequestDTO req : itemRequests) {
            Product product = productRepository.findById(req.getProductId())
                    .orElseThrow(() -> ApiException.notFound("Product", req.getProductId()));

            if (!"active".equalsIgnoreCase(product.getStatus())) {
                throw ApiException.badRequest(
                        "Product '" + product.getName() + "' is not active and cannot be ordered");
            }

            // Operator may override price; otherwise use catalogue price
            BigDecimal unitPrice = (req.getUnitPrice() != null)
                    ? req.getUnitPrice()
                    : product.getPrice();

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .productId(product.getId())
                    .sku(product.getSku())
                    .productName(product.getName())
                    .productImageUrl(product.getImageUrl())
                    .variantInfo(req.getVariantInfo())
                    .unitPrice(unitPrice)
                    .costPrice(product.getCost())
                    .itemDiscount(nullSafe(req.getItemDiscount()))
                    .taxRate(nullSafe(req.getTaxRate()))
                    .quantity(req.getQuantity())
                    .build();

            item.recalculate();
            items.add(item);
        }

        return items;
    }

    /**
     * Reserve inventory for every line item when an order is confirmed.
     * Best-effort: logs a warning rather than hard-failing if inventory record
     * doesn't exist (e.g. untracked products).
     */
    private void reserveInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getProductId() == null) continue;
            List<Inventory> records = inventoryRepository.findByProductId(item.getProductId());
            int remaining = item.getQuantity();
            for (Inventory inv : records) {
                if (remaining <= 0) break;
                int toReserve = Math.min(remaining, inv.getAvailable());
                if (toReserve > 0) {
                    inv.reserve(toReserve);
                    inventoryRepository.save(inv);
                    remaining -= toReserve;
                }
            }
            if (remaining > 0) {
                log.warn("Insufficient inventory for product {} on order {}. Shortfall: {}",
                        item.getProductId(), order.getOrderNumber(), remaining);
            }
        }
    }

    /** Release previously reserved inventory when an order is cancelled. */
    private void releaseInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getProductId() == null) continue;
            List<Inventory> records = inventoryRepository.findByProductId(item.getProductId());
            int toRelease = item.getQuantity();
            for (Inventory inv : records) {
                if (toRelease <= 0) break;
                int release = Math.min(toRelease, inv.getReserved());
                if (release > 0) {
                    inv.releaseReservation(release);
                    inventoryRepository.save(inv);
                    toRelease -= release;
                }
            }
        }
    }

    /**
     * Validate that the order's current status is one of the allowed predecessor statuses
     * before performing a transition.
     */
    private void validateTransition(Order order, OrderStatus target, OrderStatus... allowedFrom) {
        for (OrderStatus allowed : allowedFrom) {
            if (order.getStatus() == allowed) return;
        }
        throw ApiException.badRequest(
                "Cannot transition order " + order.getOrderNumber()
                + " to " + target + " from current status " + order.getStatus());
    }

    /**
     * Simple heuristic risk scorer.
     * Score 0–100; >= 70 triggers auto-flag for manual review.
     */
    private int calculateRiskScore(Order order) {
        int score = 0;
        if (order.getTotalAmount().compareTo(new BigDecimal("1000")) > 0)  score += 20;
        if (order.getTotalAmount().compareTo(new BigDecimal("5000")) > 0)  score += 30;
        if (order.getShippingAddress() != null
                && order.getBillingAddress() != null
                && !order.getShippingAddress().getCountry().equalsIgnoreCase(
                        order.getBillingAddress().getCountry()))             score += 25;
        if (order.getCustomerId() == null)                                  score += 15; // guest order
        return Math.min(score, 100);
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
