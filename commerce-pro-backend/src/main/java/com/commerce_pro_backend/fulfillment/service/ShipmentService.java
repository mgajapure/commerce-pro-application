package com.commerce_pro_backend.fulfillment.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.fulfillment.dto.*;
import com.commerce_pro_backend.fulfillment.entity.*;
import com.commerce_pro_backend.fulfillment.enums.ShipmentStatus;
import com.commerce_pro_backend.fulfillment.mapper.ShipmentMapper;
import com.commerce_pro_backend.fulfillment.repository.*;
import com.commerce_pro_backend.fulfillment.specification.ShipmentSpecification;
import com.commerce_pro_backend.order.entity.Order;
import com.commerce_pro_backend.order.enums.OrderStatus;
import com.commerce_pro_backend.order.repository.OrderRepository;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShipmentService {

    private final ShipmentRepository      shipmentRepository;
    private final ShipmentNumberService   shipmentNumberService;
    private final CarrierRepository       carrierRepository;
    private final OrderRepository         orderRepository;
    private final ShipmentMapper          shipmentMapper;
    private final AuditService            auditService;
    private final CurrentUserService      currentUserService;

    // ══════════════════════════════════════════════════════════════════════════
    // READ
    // ══════════════════════════════════════════════════════════════════════════

    public PageResponse<ShipmentSummaryDTO> listShipments(ShipmentFilterDTO filter, Pageable pageable) {
        Specification<Shipment> spec = ShipmentSpecification.withFilters(
                filter.getSearch(), filter.getStatus(), filter.getCarrierId(),
                filter.getOrderId(), filter.getCreatedFrom(), filter.getCreatedTo());
        Page<Shipment> page = shipmentRepository.findAll(spec, pageable);
        return PageResponse.from(page, shipmentMapper.toSummaryDTOList(page.getContent()));
    }

    public ShipmentDTO getShipment(String id) {
        Shipment s = findShipment(id);
        Carrier carrier = s.getCarrierId() != null
                ? carrierRepository.findById(s.getCarrierId()).orElse(null) : null;
        return shipmentMapper.toDTO(s, carrier);
    }

    public List<ShipmentSummaryDTO> getShipmentsByOrder(String orderId) {
        return shipmentMapper.toSummaryDTOList(shipmentRepository.findByOrderId(orderId));
    }

    public ShipmentStatsDTO getStats() {
        List<Object[]> grouped = shipmentRepository.countGroupedByStatus();
        Map<String, Long> breakdown = grouped.stream().collect(Collectors.toMap(
                arr -> ((ShipmentStatus) arr[0]).name(), arr -> (Long) arr[1]));

        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();

        return ShipmentStatsDTO.builder()
                .totalShipments(shipmentRepository.count())
                .shipmentsToday(shipmentRepository.countSince(startOfToday))
                .inTransit(shipmentRepository.countByStatus(ShipmentStatus.IN_TRANSIT))
                .outForDelivery(shipmentRepository.countByStatus(ShipmentStatus.OUT_FOR_DELIVERY))
                .delivered(shipmentRepository.countByStatus(ShipmentStatus.DELIVERED))
                .exceptions(shipmentRepository.countByStatus(ShipmentStatus.EXCEPTION))
                .overdue(shipmentRepository.countOverdue(ShipmentStatus.IN_TRANSIT))
                .statusBreakdown(breakdown)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CREATE
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public ShipmentDTO createShipment(CreateShipmentRequest request) {
        String actorId = currentUserService.getCurrentUserId();

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> ApiException.notFound("Order", request.getOrderId()));

        // Resolve carrier
        Carrier carrier = null;
        String carrierName = null;
        if (request.getCarrierId() != null) {
            carrier = carrierRepository.findById(request.getCarrierId())
                    .orElseThrow(() -> ApiException.notFound("Carrier", request.getCarrierId()));
            carrierName = carrier.getName();
        }

        // Default recipient from order shipping address if not provided
        String recipientName = defaultStr(request.getRecipientName(),
                order.getShippingAddress() != null ? order.getShippingAddress().getFullName() : order.getCustomerName());
        String addressLine1  = defaultStr(request.getAddressLine1(),
                order.getShippingAddress() != null ? order.getShippingAddress().getAddressLine1() : "");
        String city          = defaultStr(request.getCity(),
                order.getShippingAddress() != null ? order.getShippingAddress().getCity() : "");
        String state         = defaultStr(request.getState(),
                order.getShippingAddress() != null ? order.getShippingAddress().getState() : "");
        String postalCode    = defaultStr(request.getPostalCode(),
                order.getShippingAddress() != null ? order.getShippingAddress().getPostalCode() : "");
        String country       = defaultStr(request.getCountry(),
                order.getShippingAddress() != null ? order.getShippingAddress().getCountry() : "");

        Shipment shipment = Shipment.builder()
                .shipmentNumber(shipmentNumberService.generate())
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .carrierId(request.getCarrierId())
                .carrierName(carrierName)
                .trackingNumber(request.getTrackingNumber())
                .status(ShipmentStatus.LABEL_CREATED)
                .shippingMethod(request.getShippingMethod())
                .serviceLevel(request.getServiceLevel())
                .estimatedDeliveryDate(request.getEstimatedDeliveryDate())
                .recipientName(recipientName)
                .recipientPhone(request.getRecipientPhone())
                .addressLine1(addressLine1)
                .addressLine2(request.getAddressLine2())
                .city(city)
                .state(state)
                .postalCode(postalCode)
                .country(country)
                .weightGrams(request.getWeightGrams())
                .lengthCm(request.getLengthCm())
                .widthCm(request.getWidthCm())
                .heightCm(request.getHeightCm())
                .shippingCost(request.getShippingCost())
                .notes(request.getNotes())
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        Shipment saved = shipmentRepository.save(shipment);

        // Sync tracking info onto the order
        syncTrackingToOrder(order, saved);

        auditService.log(actorId, AuditAction.FULFILLMENT_SHIPMENT_CREATED,
                "SHIPMENT", saved.getId(), saved.getShipmentNumber(), null, null, true);
        log.info("Created shipment {} for order {}", saved.getShipmentNumber(), order.getOrderNumber());
        return shipmentMapper.toDTO(saved, carrier);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public ShipmentDTO updateShipment(String id, UpdateShipmentRequest request) {
        String actorId = currentUserService.getCurrentUserId();
        Shipment shipment = findShipment(id);

        Carrier carrier = null;
        if (request.getCarrierId() != null) {
            carrier = carrierRepository.findById(request.getCarrierId())
                    .orElseThrow(() -> ApiException.notFound("Carrier", request.getCarrierId()));
            shipment.setCarrierId(carrier.getId());
            shipment.setCarrierName(carrier.getName());
        }
        if (request.getTrackingNumber() != null) shipment.setTrackingNumber(request.getTrackingNumber());
        if (request.getShippingMethod()  != null) shipment.setShippingMethod(request.getShippingMethod());
        if (request.getServiceLevel()    != null) shipment.setServiceLevel(request.getServiceLevel());
        if (request.getEstimatedDeliveryDate() != null) shipment.setEstimatedDeliveryDate(request.getEstimatedDeliveryDate());
        if (request.getWeightGrams()     != null) shipment.setWeightGrams(request.getWeightGrams());
        if (request.getLengthCm()        != null) shipment.setLengthCm(request.getLengthCm());
        if (request.getWidthCm()         != null) shipment.setWidthCm(request.getWidthCm());
        if (request.getHeightCm()        != null) shipment.setHeightCm(request.getHeightCm());
        if (request.getShippingCost()    != null) shipment.setShippingCost(request.getShippingCost());
        if (request.getNotes()           != null) shipment.setNotes(request.getNotes());
        if (request.getExceptionReason() != null) shipment.setExceptionReason(request.getExceptionReason());
        shipment.setUpdatedBy(actorId);

        Shipment saved = shipmentRepository.save(shipment);
        auditService.log(actorId, AuditAction.FULFILLMENT_SHIPMENT_UPDATED,
                "SHIPMENT", saved.getId(), saved.getShipmentNumber(), null, null, true);
        return shipmentMapper.toDTO(saved, carrier);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TRACKING EVENTS
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public ShipmentDTO addTrackingEvent(String id, AddTrackingEventRequest request) {
        String actorId = currentUserService.getCurrentUserId();
        Shipment shipment = findShipment(id);

        ShipmentStatus newStatus;
        try {
            newStatus = ShipmentStatus.valueOf(request.getStatus());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Invalid shipment status: " + request.getStatus());
        }

        LocalDateTime eventTime = request.getEventTime() != null
                ? request.getEventTime() : LocalDateTime.now();

        TrackingEvent event = TrackingEvent.builder()
                .status(newStatus)
                .description(request.getDescription())
                .location(request.getLocation())
                .eventTime(eventTime)
                .createdBy(actorId)
                .build();

        shipment.addTrackingEvent(event); // also updates shipment.status
        shipment.setUpdatedBy(actorId);

        // Propagate status changes to order
        Order order = orderRepository.findById(shipment.getOrderId()).orElse(null);
        if (order != null) {
            if (newStatus == ShipmentStatus.PICKED_UP && order.getStatus() != OrderStatus.SHIPPED) {
                order.transitionTo(OrderStatus.SHIPPED, actorId, "Shipment picked up by carrier");
                orderRepository.save(order);
            } else if (newStatus == ShipmentStatus.OUT_FOR_DELIVERY) {
                order.transitionTo(OrderStatus.OUT_FOR_DELIVERY, actorId, "Out for delivery");
                orderRepository.save(order);
            } else if (newStatus == ShipmentStatus.DELIVERED) {
                order.transitionTo(OrderStatus.DELIVERED, actorId, "Delivered — shipment " + shipment.getShipmentNumber());
                orderRepository.save(order);
            }
        }

        Shipment saved = shipmentRepository.save(shipment);

        Carrier carrier = shipment.getCarrierId() != null
                ? carrierRepository.findById(shipment.getCarrierId()).orElse(null) : null;

        auditService.log(actorId, AuditAction.FULFILLMENT_SHIPMENT_TRACKING_ADDED,
                "SHIPMENT", saved.getId(), saved.getShipmentNumber(), null, newStatus.name(), true);
        return shipmentMapper.toDTO(saved, carrier);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELIVER (shortcut)
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public ShipmentDTO markDelivered(String id) {
        AddTrackingEventRequest req = new AddTrackingEventRequest();
        req.setStatus(ShipmentStatus.DELIVERED.name());
        req.setDescription("Delivered to recipient");
        req.setEventTime(LocalDateTime.now());
        return addTrackingEvent(id, req);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public void deleteShipment(String id) {
        String actorId = currentUserService.getCurrentUserId();
        Shipment shipment = findShipment(id);
        if (!shipment.isDeletable()) {
            throw ApiException.badRequest("Only LABEL_CREATED shipments can be deleted. Current: " + shipment.getStatus());
        }
        shipmentRepository.delete(shipment);
        auditService.log(actorId, AuditAction.FULFILLMENT_SHIPMENT_DELETED,
                "SHIPMENT", id, shipment.getShipmentNumber(), null, null, true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private Shipment findShipment(String id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Shipment", id));
    }

    private void syncTrackingToOrder(Order order, Shipment shipment) {
        if (shipment.getTrackingNumber() != null) {
            order.setTrackingNumber(shipment.getTrackingNumber());
        }
        if (shipment.getCarrierName() != null) {
            order.setCarrier(shipment.getCarrierName());
        }
        orderRepository.save(order);
    }

    private String defaultStr(String preferred, String fallback) {
        return (preferred != null && !preferred.isBlank()) ? preferred : fallback;
    }
}
