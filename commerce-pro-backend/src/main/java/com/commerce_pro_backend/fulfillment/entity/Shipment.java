package com.commerce_pro_backend.fulfillment.entity;

import com.commerce_pro_backend.fulfillment.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shipments", indexes = {
        @Index(name = "idx_shipment_number",   columnList = "shipment_number",  unique = true),
        @Index(name = "idx_shipment_order",    columnList = "order_id"),
        @Index(name = "idx_shipment_status",   columnList = "status"),
        @Index(name = "idx_shipment_tracking", columnList = "tracking_number"),
        @Index(name = "idx_shipment_carrier",  columnList = "carrier_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(name = "shipment_number", nullable = false, unique = true, length = 30)
    private String shipmentNumber;

    // ── Order reference ───────────────────────────────────────────────────────
    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "order_number", nullable = false, length = 30)
    private String orderNumber;

    // ── Carrier ───────────────────────────────────────────────────────────────
    @Column(name = "carrier_id", length = 36)
    private String carrierId;

    @Column(name = "carrier_name", length = 100)
    private String carrierName;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ShipmentStatus status = ShipmentStatus.LABEL_CREATED;

    @Column(name = "shipping_method", length = 100)
    private String shippingMethod;

    @Column(name = "service_level", length = 100)
    private String serviceLevel;

    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    // ── Recipient address (denormalized from order at shipment time) ──────────
    @Column(name = "recipient_name", nullable = false, length = 200)
    private String recipientName;

    @Column(name = "recipient_phone", length = 30)
    private String recipientPhone;

    @Column(name = "address_line1", nullable = false, length = 300)
    private String addressLine1;

    @Column(name = "address_line2", length = 300)
    private String addressLine2;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(nullable = false, length = 100)
    private String country;

    // ── Parcel dimensions ─────────────────────────────────────────────────────
    @Column(name = "weight_grams")
    private Integer weightGrams;

    @Column(name = "length_cm")
    private Integer lengthCm;

    @Column(name = "width_cm")
    private Integer widthCm;

    @Column(name = "height_cm")
    private Integer heightCm;

    // ── Cost ──────────────────────────────────────────────────────────────────
    @Column(name = "shipping_cost", precision = 19, scale = 4)
    private BigDecimal shippingCost;

    // ── Label / notes ─────────────────────────────────────────────────────────
    @Column(name = "label_url", columnDefinition = "TEXT")
    private String labelUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "exception_reason", length = 500)
    private String exceptionReason;

    // ── Audit ─────────────────────────────────────────────────────────────────
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

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("eventTime DESC")
    @Builder.Default
    private List<TrackingEvent> trackingEvents = new ArrayList<>();

    // ── Business logic ────────────────────────────────────────────────────────

    public void addTrackingEvent(TrackingEvent event) {
        event.setShipment(this);
        this.trackingEvents.add(event);
        this.status = event.getStatus();

        if (event.getStatus() == ShipmentStatus.DELIVERED && this.actualDeliveryDate == null) {
            this.actualDeliveryDate = event.getEventTime().toLocalDate();
        }
    }

    public boolean isDeletable() {
        return status == ShipmentStatus.LABEL_CREATED;
    }

    public String getTrackingUrl(String template) {
        if (template == null || trackingNumber == null) return null;
        return template.replace("{trackingNumber}", trackingNumber);
    }
}
