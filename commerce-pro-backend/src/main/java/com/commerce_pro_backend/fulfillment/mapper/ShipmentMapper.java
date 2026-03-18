package com.commerce_pro_backend.fulfillment.mapper;

import com.commerce_pro_backend.fulfillment.dto.ShipmentDTO;
import com.commerce_pro_backend.fulfillment.dto.ShipmentSummaryDTO;
import com.commerce_pro_backend.fulfillment.dto.TrackingEventDTO;
import com.commerce_pro_backend.fulfillment.entity.Carrier;
import com.commerce_pro_backend.fulfillment.entity.Shipment;
import com.commerce_pro_backend.fulfillment.entity.TrackingEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ShipmentMapper {

    public ShipmentSummaryDTO toSummaryDTO(Shipment s) {
        if (s == null) return null;
        return ShipmentSummaryDTO.builder()
                .id(s.getId())
                .shipmentNumber(s.getShipmentNumber())
                .orderId(s.getOrderId())
                .orderNumber(s.getOrderNumber())
                .carrierName(s.getCarrierName())
                .trackingNumber(s.getTrackingNumber())
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .shippingMethod(s.getShippingMethod())
                .estimatedDeliveryDate(s.getEstimatedDeliveryDate())
                .actualDeliveryDate(s.getActualDeliveryDate())
                .recipientName(s.getRecipientName())
                .city(s.getCity())
                .country(s.getCountry())
                .shippingCost(s.getShippingCost())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    public List<ShipmentSummaryDTO> toSummaryDTOList(List<Shipment> list) {
        if (list == null) return List.of();
        return list.stream().map(this::toSummaryDTO).collect(Collectors.toList());
    }

    public ShipmentDTO toDTO(Shipment s, Carrier carrier) {
        if (s == null) return null;
        String trackingUrl = null;
        if (carrier != null && carrier.getTrackingUrlTemplate() != null && s.getTrackingNumber() != null) {
            trackingUrl = s.getTrackingUrl(carrier.getTrackingUrlTemplate());
        }
        return ShipmentDTO.builder()
                .id(s.getId())
                .shipmentNumber(s.getShipmentNumber())
                .orderId(s.getOrderId())
                .orderNumber(s.getOrderNumber())
                .carrierId(s.getCarrierId())
                .carrierName(s.getCarrierName())
                .trackingNumber(s.getTrackingNumber())
                .trackingUrl(trackingUrl)
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .shippingMethod(s.getShippingMethod())
                .serviceLevel(s.getServiceLevel())
                .estimatedDeliveryDate(s.getEstimatedDeliveryDate())
                .actualDeliveryDate(s.getActualDeliveryDate())
                .recipientName(s.getRecipientName())
                .recipientPhone(s.getRecipientPhone())
                .addressLine1(s.getAddressLine1())
                .addressLine2(s.getAddressLine2())
                .city(s.getCity())
                .state(s.getState())
                .postalCode(s.getPostalCode())
                .country(s.getCountry())
                .weightGrams(s.getWeightGrams())
                .lengthCm(s.getLengthCm())
                .widthCm(s.getWidthCm())
                .heightCm(s.getHeightCm())
                .shippingCost(s.getShippingCost())
                .labelUrl(s.getLabelUrl())
                .notes(s.getNotes())
                .exceptionReason(s.getExceptionReason())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .createdBy(s.getCreatedBy())
                .trackingEvents(toEventDTOList(s.getTrackingEvents()))
                .build();
    }

    public TrackingEventDTO toEventDTO(TrackingEvent e) {
        if (e == null) return null;
        return TrackingEventDTO.builder()
                .id(e.getId())
                .status(e.getStatus() != null ? e.getStatus().name() : null)
                .location(e.getLocation())
                .description(e.getDescription())
                .eventTime(e.getEventTime())
                .createdAt(e.getCreatedAt())
                .createdBy(e.getCreatedBy())
                .build();
    }

    public List<TrackingEventDTO> toEventDTOList(List<TrackingEvent> events) {
        if (events == null) return List.of();
        return events.stream().map(this::toEventDTO).collect(Collectors.toList());
    }
}
