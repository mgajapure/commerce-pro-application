package com.commerce_pro_backend.inventory.forecast.service;

import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.inventory.forecast.dto.DemandForecastDto;
import com.commerce_pro_backend.inventory.forecast.entity.DemandForecast;
import com.commerce_pro_backend.inventory.forecast.mapper.DemandForecastMapper;
import com.commerce_pro_backend.inventory.forecast.repository.DemandForecastRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for Demand Forecasting business logic.
 *
 * <p>Forecast generation uses a simple algorithm family:
 * <ul>
 *   <li>MOVING_AVERAGE / WEIGHTED_MOVING_AVERAGE — multiplies baseline demand by period length</li>
 *   <li>EXPONENTIAL_SMOOTHING — applies a 0.3 smoothing factor to baseline demand</li>
 *   <li>LINEAR_REGRESSION — adds a 5 % linear growth trend per period to baseline demand</li>
 * </ul>
 * Safety stock = variabilityFactor * sqrt(periodDays) * averageDailyDemand (approximation).
 * Reorder point = (averageDailyDemand * leadTimeDays) + safetyStock, using a fixed lead time of 7 days.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DemandForecastService {

    private static final double DEFAULT_SMOOTHING_FACTOR = 0.3;
    private static final double DEFAULT_GROWTH_RATE = 0.05;
    private static final int DEFAULT_LEAD_TIME_DAYS = 7;
    private static final double DEFAULT_VARIABILITY_FACTOR = 0.25;

    private final DemandForecastRepository forecastRepository;
    private final DemandForecastMapper forecastMapper;

    // ==================== READ OPERATIONS ====================

    public PageResponse<DemandForecastDto.Response> getAllForecasts(Pageable pageable) {
        Page<DemandForecast> page = forecastRepository.findAll(pageable);
        List<DemandForecastDto.Response> content = page.getContent()
                .stream()
                .map(forecastMapper::toResponse)
                .collect(Collectors.toList());
        return PageResponse.from(page, content);
    }

    public DemandForecastDto.Response getForecastById(String id) {
        DemandForecast forecast = findOrThrow(id);
        return forecastMapper.toResponse(forecast);
    }

    public List<DemandForecastDto.Response> getForecastsByProduct(String productId) {
        return forecastRepository.findByProductId(productId)
                .stream()
                .map(forecastMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<DemandForecastDto.Response> getForecastsByWarehouse(String warehouseId) {
        return forecastRepository.findByWarehouseId(warehouseId)
                .stream()
                .map(forecastMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<DemandForecastDto.Response> getForecastsByStatus(String status) {
        return forecastRepository.findAllByStatusOrderByGeneratedAtDesc(status)
                .stream()
                .map(forecastMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<DemandForecastDto.Response> getForecastsByPeriod(String period) {
        return forecastRepository.findByPeriod(period)
                .stream()
                .map(forecastMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ==================== WRITE OPERATIONS ====================

    @Transactional
    public DemandForecastDto.Response createForecast(DemandForecastDto.Request request) {
        log.info("Creating demand forecast for productId={}", request.getProductId());
        DemandForecast entity = forecastMapper.toEntity(request);
        DemandForecast saved = forecastRepository.save(entity);
        return forecastMapper.toResponse(saved);
    }

    @Transactional
    public DemandForecastDto.Response updateForecast(String id, DemandForecastDto.Request request) {
        log.info("Updating demand forecast id={}", id);
        DemandForecast existing = findOrThrow(id);
        forecastMapper.updateEntityFromRequest(request, existing);
        DemandForecast saved = forecastRepository.save(existing);
        return forecastMapper.toResponse(saved);
    }

    @Transactional
    public void deleteForecast(String id) {
        log.info("Deleting demand forecast id={}", id);
        DemandForecast forecast = findOrThrow(id);
        forecastRepository.delete(forecast);
    }

    // ==================== STATUS TRANSITIONS ====================

    @Transactional
    public DemandForecastDto.Response archiveForecast(String id) {
        log.info("Archiving demand forecast id={}", id);
        DemandForecast forecast = findOrThrow(id);
        if ("archived".equals(forecast.getStatus())) {
            throw ApiException.badRequest("Forecast is already archived");
        }
        forecast.setStatus("archived");
        return forecastMapper.toResponse(forecastRepository.save(forecast));
    }

    @Transactional
    public DemandForecastDto.Response markObsolete(String id) {
        log.info("Marking demand forecast id={} as obsolete", id);
        DemandForecast forecast = findOrThrow(id);
        if ("obsolete".equals(forecast.getStatus())) {
            throw ApiException.badRequest("Forecast is already marked as obsolete");
        }
        forecast.setStatus("obsolete");
        return forecastMapper.toResponse(forecastRepository.save(forecast));
    }

    // ==================== FORECAST GENERATION ====================

    @Transactional
    public DemandForecastDto.Response generateForecast(DemandForecastDto.GenerateRequest request) {
        log.info("Generating demand forecast for productId={}, period={}, algorithm={}",
                request.getProductId(), request.getPeriod(), request.getAlgorithm());

        String algorithm = request.getAlgorithm() != null ? request.getAlgorithm() : "MOVING_AVERAGE";
        double baselineDailyDemand = request.getBaselineDailyDemand() != null
                ? request.getBaselineDailyDemand() : 10.0;
        double variabilityFactor = request.getDemandVariabilityFactor() != null
                ? request.getDemandVariabilityFactor() : DEFAULT_VARIABILITY_FACTOR;

        long periodDays = computePeriodDays(request.getStartDate(), request.getEndDate(), request.getPeriod());

        double averageDailyDemand = computeAverageDailyDemand(algorithm, baselineDailyDemand, periodDays);
        int totalPredictedDemand = (int) Math.round(averageDailyDemand * periodDays);
        int peakDemandQuantity = (int) Math.round(averageDailyDemand * 1.5);
        int safetyStock = computeSafetyStock(averageDailyDemand, periodDays, variabilityFactor);
        int reorderPoint = (int) Math.round(averageDailyDemand * DEFAULT_LEAD_TIME_DAYS) + safetyStock;

        DemandForecast forecast = DemandForecast.builder()
                .productId(request.getProductId())
                .productName(request.getProductName())
                .sku(request.getSku())
                .warehouseId(request.getWarehouseId())
                .warehouseName(request.getWarehouseName())
                .period(request.getPeriod())
                .algorithm(algorithm)
                .status("active")
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalPredictedDemand(totalPredictedDemand)
                .averageDailyDemand(round(averageDailyDemand, 2))
                .peakDemandQuantity(peakDemandQuantity)
                .safetyStockRecommendation(safetyStock)
                .reorderPointRecommendation(reorderPoint)
                .generatedBy(request.getGeneratedBy() != null ? request.getGeneratedBy() : "SYSTEM")
                .build();

        DemandForecast saved = forecastRepository.save(forecast);
        log.info("Demand forecast generated: id={}, totalPredicted={}", saved.getId(), totalPredictedDemand);
        return forecastMapper.toResponse(saved);
    }

    // ==================== PRIVATE HELPERS ====================

    private DemandForecast findOrThrow(String id) {
        return forecastRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("DemandForecast", id));
    }

    /**
     * Computes average daily demand using the specified algorithm.
     */
    private double computeAverageDailyDemand(String algorithm, double baseline, long periodDays) {
        return switch (algorithm) {
            case "EXPONENTIAL_SMOOTHING" ->
                    DEFAULT_SMOOTHING_FACTOR * baseline + (1 - DEFAULT_SMOOTHING_FACTOR) * baseline;
            case "LINEAR_REGRESSION" -> {
                // Simple linear trend: demand grows by DEFAULT_GROWTH_RATE per period
                double periods = Math.max(1, periodDays / 30.0);
                yield baseline * (1 + DEFAULT_GROWTH_RATE * periods);
            }
            case "WEIGHTED_MOVING_AVERAGE" ->
                    // Weighted: recent data weighted 60 %, older 40 % — simplified to 1.1x baseline
                    baseline * 1.1;
            default -> // MOVING_AVERAGE — straight baseline
                    baseline;
        };
    }

    /**
     * Approximates safety stock: variabilityFactor * sqrt(periodDays) * avgDailyDemand.
     */
    private int computeSafetyStock(double avgDailyDemand, long periodDays, double variabilityFactor) {
        double safetyStock = variabilityFactor * Math.sqrt(periodDays) * avgDailyDemand;
        return (int) Math.ceil(safetyStock);
    }

    /**
     * Determines the effective number of days in the forecast window.
     * Falls back to a period-based estimate when dates produce an unreasonable span.
     */
    private long computePeriodDays(Instant start, Instant end, String period) {
        if (start != null && end != null && end.isAfter(start)) {
            long days = Duration.between(start, end).toDays();
            if (days > 0) {
                return days;
            }
        }
        return switch (period) {
            case "daily" -> 1;
            case "weekly" -> 7;
            case "monthly" -> 30;
            case "quarterly" -> 90;
            case "yearly" -> 365;
            default -> 30;
        };
    }

    private double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}
