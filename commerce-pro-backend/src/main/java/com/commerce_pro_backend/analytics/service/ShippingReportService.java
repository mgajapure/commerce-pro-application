package com.commerce_pro_backend.analytics.service;

import com.commerce_pro_backend.analytics.dto.ReportFilterDTO;
import com.commerce_pro_backend.analytics.dto.ShippingReportDTO;
import com.commerce_pro_backend.analytics.dto.ShippingReportDTO.*;
import com.commerce_pro_backend.fulfillment.entity.Shipment;
import com.commerce_pro_backend.fulfillment.enums.ShipmentStatus;
import com.commerce_pro_backend.fulfillment.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ShippingReportService {

    private final ShipmentRepository shipmentRepository;

    public ShippingReportDTO getReport(ReportFilterDTO filter) {
        LocalDateTime[] range = DateRangeHelper.resolve(filter);

        List<Shipment> shipments = filter.getCarrierIds() != null && !filter.getCarrierIds().isEmpty()
                ? shipmentRepository.findByCarrierIdInAndCreatedAtBetween(filter.getCarrierIds(), range[0], range[1])
                : shipmentRepository.findByCreatedAtBetween(range[0], range[1]);

        long total          = shipments.size();
        long delivered      = shipments.stream().filter(s -> s.getStatus() == ShipmentStatus.DELIVERED).count();
        long onTime         = shipments.stream().filter(this::isOnTime).count();
        long late           = shipments.stream().filter(s -> s.getStatus() == ShipmentStatus.DELIVERED && !isOnTime(s)).count();
        long inTransit      = shipments.stream().filter(s -> s.getStatus() == ShipmentStatus.IN_TRANSIT || s.getStatus() == ShipmentStatus.PICKED_UP || s.getStatus() == ShipmentStatus.OUT_FOR_DELIVERY).count();
        long lost           = shipments.stream().filter(s -> s.getStatus() == ShipmentStatus.LOST).count();
        long returned       = shipments.stream().filter(s -> s.getStatus() == ShipmentStatus.RETURNED_TO_SENDER).count();

        BigDecimal onTimeRate = total > 0
                ? BigDecimal.valueOf(onTime).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Average delivery days (only delivered shipments with both dates)
        OptionalDouble avgDays = shipments.stream()
                .filter(s -> s.getStatus() == ShipmentStatus.DELIVERED
                        && s.getActualDeliveryDate() != null && s.getCreatedAt() != null)
                .mapToLong(s -> ChronoUnit.DAYS.between(s.getCreatedAt().toLocalDate(), s.getActualDeliveryDate()))
                .filter(d -> d >= 0)
                .average();

        // Per-carrier aggregation
        Map<String, List<Shipment>> byCarrier = shipments.stream()
                .filter(s -> s.getCarrierName() != null)
                .collect(Collectors.groupingBy(s -> s.getCarrierId() != null ? s.getCarrierId() : s.getCarrierName()));

        List<CarrierPerformanceDTO> carrierPerf = byCarrier.entrySet().stream()
                .map(e -> {
                    List<Shipment> cs  = e.getValue();
                    long cTotal        = cs.size();
                    long cDelivered    = cs.stream().filter(s -> s.getStatus() == ShipmentStatus.DELIVERED).count();
                    long cOnTime       = cs.stream().filter(this::isOnTime).count();
                    long cLate         = cDelivered - cOnTime;
                    long cLost         = cs.stream().filter(s -> s.getStatus() == ShipmentStatus.LOST).count();
                    long cReturned     = cs.stream().filter(s -> s.getStatus() == ShipmentStatus.RETURNED_TO_SENDER).count();
                    BigDecimal cRate   = cTotal > 0
                            ? BigDecimal.valueOf(cOnTime).divide(BigDecimal.valueOf(cTotal), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;
                    BigDecimal totalCost = cs.stream().filter(s -> s.getShippingCost() != null)
                            .map(Shipment::getShippingCost).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avgCost = cTotal > 0
                            ? totalCost.divide(BigDecimal.valueOf(cTotal), 4, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    OptionalDouble cAvgDays = cs.stream()
                            .filter(s -> s.getStatus() == ShipmentStatus.DELIVERED
                                    && s.getActualDeliveryDate() != null && s.getCreatedAt() != null)
                            .mapToLong(s -> ChronoUnit.DAYS.between(s.getCreatedAt().toLocalDate(), s.getActualDeliveryDate()))
                            .filter(d -> d >= 0).average();
                    OptionalDouble cAvgWeight = cs.stream()
                            .filter(s -> s.getWeightGrams() != null)
                            .mapToInt(Shipment::getWeightGrams).average();
                    String carrierName = cs.get(0).getCarrierName();

                    return CarrierPerformanceDTO.builder()
                            .carrierId(e.getKey()).carrierName(carrierName)
                            .totalShipments(cTotal).delivered(cDelivered)
                            .onTime(cOnTime).late(Math.max(0, cLate))
                            .lost(cLost).returnedToSender(cReturned)
                            .onTimeRatePct(cRate)
                            .avgDeliveryDays(cAvgDays.isPresent() ? BigDecimal.valueOf(cAvgDays.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                            .totalShippingCost(totalCost).avgShippingCost(avgCost)
                            .avgWeightGrams(cAvgWeight.isPresent() ? BigDecimal.valueOf(cAvgWeight.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                            .build();
                })
                .sorted(Comparator.comparing(CarrierPerformanceDTO::getTotalShipments).reversed())
                .toList();

        // Detail rows
        List<ShipmentDetailRowDTO> rows = shipments.stream()
                .sorted(Comparator.comparing(Shipment::getCreatedAt).reversed())
                .map(s -> {
                    Long delivDays = null;
                    Boolean ot = null;
                    if (s.getStatus() == ShipmentStatus.DELIVERED && s.getActualDeliveryDate() != null && s.getCreatedAt() != null) {
                        delivDays = ChronoUnit.DAYS.between(s.getCreatedAt().toLocalDate(), s.getActualDeliveryDate());
                        ot = isOnTime(s);
                    }
                    return ShipmentDetailRowDTO.builder()
                            .shipmentNumber(s.getShipmentNumber())
                            .orderNumber(s.getOrderNumber())
                            .carrierName(s.getCarrierName())
                            .trackingNumber(s.getTrackingNumber())
                            .status(s.getStatus().name())
                            .shippedAt(s.getCreatedAt() != null ? s.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                            .estimatedDeliveryDate(s.getEstimatedDeliveryDate() != null ? s.getEstimatedDeliveryDate().toString() : null)
                            .actualDeliveryDate(s.getActualDeliveryDate() != null ? s.getActualDeliveryDate().toString() : null)
                            .deliveryDays(delivDays != null ? delivDays.intValue() : null)
                            .onTime(ot)
                            .shippingCost(s.getShippingCost())
                            .recipientCity(s.getCity())
                            .recipientCountry(s.getCountry())
                            .build();
                }).toList();

        // Trend
        String groupBy = filter.getGroupBy() != null ? filter.getGroupBy().toUpperCase() : "DAY";
        Map<String, List<Shipment>> grouped = shipments.stream()
                .collect(Collectors.groupingBy(s -> periodKey(s.getCreatedAt(), groupBy)));
        List<ShippingTrendRowDTO> trend = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    List<Shipment> ps = e.getValue();
                    long pTotal = ps.size();
                    long pOnTime = ps.stream().filter(this::isOnTime).count();
                    BigDecimal pRate = pTotal > 0
                            ? BigDecimal.valueOf(pOnTime).divide(BigDecimal.valueOf(pTotal), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;
                    BigDecimal pCost = ps.stream().filter(s -> s.getShippingCost() != null)
                            .map(Shipment::getShippingCost).reduce(BigDecimal.ZERO, BigDecimal::add);
                    OptionalDouble pAvgDays = ps.stream()
                            .filter(s -> s.getStatus() == ShipmentStatus.DELIVERED && s.getActualDeliveryDate() != null && s.getCreatedAt() != null)
                            .mapToLong(s -> ChronoUnit.DAYS.between(s.getCreatedAt().toLocalDate(), s.getActualDeliveryDate()))
                            .filter(d -> d >= 0).average();
                    return ShippingTrendRowDTO.builder()
                            .period(e.getKey()).shipments(pTotal).onTimeRatePct(pRate)
                            .avgDeliveryDays(pAvgDays.isPresent() ? BigDecimal.valueOf(pAvgDays.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                            .totalCost(pCost).build();
                }).toList();

        return ShippingReportDTO.builder()
                .totalShipments(total).deliveredShipments(delivered)
                .onTimeDeliveries(onTime).lateDeliveries(late)
                .inTransit(inTransit).lost(lost).returnedToSender(returned)
                .onTimeRatePct(onTimeRate)
                .averageDeliveryDays(avgDays.isPresent() ? BigDecimal.valueOf(avgDays.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .carrierPerformance(carrierPerf)
                .rows(rows).totalRows(total)
                .trend(trend)
                .build();
    }

    private boolean isOnTime(Shipment s) {
        return s.getStatus() == ShipmentStatus.DELIVERED
                && s.getEstimatedDeliveryDate() != null
                && s.getActualDeliveryDate() != null
                && !s.getActualDeliveryDate().isAfter(s.getEstimatedDeliveryDate());
    }

    private String periodKey(LocalDateTime dt, String groupBy) {
        if (dt == null) return "unknown";
        return switch (groupBy) {
            case "WEEK"    -> dt.getYear() + "-W" + String.format("%02d", dt.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            case "MONTH"   -> dt.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            case "QUARTER" -> dt.getYear() + "-Q" + ((dt.getMonthValue() - 1) / 3 + 1);
            case "YEAR"    -> String.valueOf(dt.getYear());
            default        -> dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        };
    }
}
