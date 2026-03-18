package com.commerce_pro_backend.order.entity;

import com.commerce_pro_backend.order.enums.FulfillmentStatus;
import com.commerce_pro_backend.order.enums.OrderSource;
import com.commerce_pro_backend.order.enums.OrderStatus;
import com.commerce_pro_backend.order.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order entity — the aggregate root of the Order Management module.
 * Monetary fields use BigDecimal(precision=19, scale=4) per project standards.
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_number",       columnList = "order_number", unique = true),
        @Index(name = "idx_order_status",       columnList = "status"),
        @Index(name = "idx_order_customer",     columnList = "customer_id"),
        @Index(name = "idx_order_created_at",   columnList = "created_at"),
        @Index(name = "idx_order_payment_status", columnList = "payment_status"),
        @Index(name = "idx_order_source",       columnList = "source")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    // ── Reference ────────────────────────────────────────────────────────────
    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    // ── Customer info (denormalised at order time, no FK to allow guest orders)
    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "customer_email", nullable = false, length = 255)
    private String customerEmail;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    // ── Status ────────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_status", nullable = false, length = 30)
    @Builder.Default
    private FulfillmentStatus fulfillmentStatus = FulfillmentStatus.UNFULFILLED;

    // ── Source ────────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderSource source = OrderSource.MANUAL;

    // ── Financial totals (precision=19 scale=4) ───────────────────────────────
    @Column(name = "subtotal", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "shipping_cost", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "refunded_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    // ── Coupon / discount ─────────────────────────────────────────────────────
    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    // ── Currency ─────────────────────────────────────────────────────────────
    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "USD";

    // ── Shipping address (embedded) ───────────────────────────────────────────
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fullName",   column = @Column(name = "ship_full_name",    length = 200)),
            @AttributeOverride(name = "addressLine1", column = @Column(name = "ship_address1",   length = 300)),
            @AttributeOverride(name = "addressLine2", column = @Column(name = "ship_address2",   length = 300)),
            @AttributeOverride(name = "city",       column = @Column(name = "ship_city",         length = 100)),
            @AttributeOverride(name = "state",      column = @Column(name = "ship_state",        length = 100)),
            @AttributeOverride(name = "postalCode", column = @Column(name = "ship_postal_code",  length = 20)),
            @AttributeOverride(name = "country",    column = @Column(name = "ship_country",      length = 100)),
            @AttributeOverride(name = "phone",      column = @Column(name = "ship_phone",        length = 30))
    })
    private OrderAddress shippingAddress;

    // ── Billing address (embedded) ────────────────────────────────────────────
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fullName",   column = @Column(name = "bill_full_name",    length = 200)),
            @AttributeOverride(name = "addressLine1", column = @Column(name = "bill_address1",   length = 300)),
            @AttributeOverride(name = "addressLine2", column = @Column(name = "bill_address2",   length = 300)),
            @AttributeOverride(name = "city",       column = @Column(name = "bill_city",         length = 100)),
            @AttributeOverride(name = "state",      column = @Column(name = "bill_state",        length = 100)),
            @AttributeOverride(name = "postalCode", column = @Column(name = "bill_postal_code",  length = 20)),
            @AttributeOverride(name = "country",    column = @Column(name = "bill_country",      length = 100)),
            @AttributeOverride(name = "phone",      column = @Column(name = "bill_phone",        length = 30))
    })
    private OrderAddress billingAddress;

    // ── Shipping method ───────────────────────────────────────────────────────
    @Column(name = "shipping_method", length = 100)
    private String shippingMethod;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "carrier", length = 100)
    private String carrier;

    // ── Hold / notes ──────────────────────────────────────────────────────────
    @Column(name = "hold_reason", length = 500)
    private String holdReason;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "internal_notes", length = 2000)
    private String internalNotes;

    @Column(name = "customer_notes", length = 1000)
    private String customerNotes;

    // ── Risk ──────────────────────────────────────────────────────────────────
    @Column(name = "risk_score")
    @Builder.Default
    private Integer riskScore = 0;

    @Column(name = "is_flagged")
    @Builder.Default
    private Boolean isFlagged = false;

    // ── Important dates ───────────────────────────────────────────────────────
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // ── Audit ────────────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false, length = 36)
    private String createdBy;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    // ── Relationships ─────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    // ── Business methods ──────────────────────────────────────────────────────

    /**
     * Recompute totals from current items. Call after any item change.
     */
    public void recalculateTotals() {
        this.subtotal = items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalAmount = subtotal
                .subtract(discountAmount)
                .add(shippingCost)
                .add(taxAmount);

        if (this.totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.totalAmount = BigDecimal.ZERO;
        }
    }

    /** Transition to a new status and record it. */
    public void transitionTo(OrderStatus newStatus, String actorId, String reason) {
        OrderStatusHistory entry = OrderStatusHistory.builder()
                .order(this)
                .previousStatus(this.status)
                .newStatus(newStatus)
                .reason(reason)
                .changedBy(actorId)
                .build();
        this.statusHistory.add(entry);
        this.status = newStatus;

        // Set milestone timestamps
        switch (newStatus) {
            case CONFIRMED              -> this.confirmedAt  = LocalDateTime.now();
            case SHIPPED                -> this.shippedAt    = LocalDateTime.now();
            case DELIVERED              -> this.deliveredAt  = LocalDateTime.now();
            case CANCELLED              -> this.cancelledAt  = LocalDateTime.now();
            case CLOSED                 -> this.closedAt     = LocalDateTime.now();
            default -> { /* no timestamp side-effect */ }
        }
    }

    /** Whether the order is still modifiable (pre-fulfillment). */
    public boolean isEditable() {
        return status == OrderStatus.DRAFT
                || status == OrderStatus.PENDING_PAYMENT
                || status == OrderStatus.CONFIRMED;
    }

    /** Whether the order can be cancelled. */
    public boolean isCancellable() {
        return status != OrderStatus.SHIPPED
                && status != OrderStatus.OUT_FOR_DELIVERY
                && status != OrderStatus.DELIVERED
                && status != OrderStatus.CANCELLED
                && status != OrderStatus.CLOSED;
    }

    /** Total quantity of all line items. */
    public int getTotalQuantity() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }
}
