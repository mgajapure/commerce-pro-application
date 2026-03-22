package com.commerce_pro_backend.inventory.forecast.repository;

import com.commerce_pro_backend.inventory.forecast.entity.DemandForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for DemandForecast entity.
 */
@Repository
public interface DemandForecastRepository extends JpaRepository<DemandForecast, String> {

    List<DemandForecast> findByProductId(String productId);

    List<DemandForecast> findByWarehouseId(String warehouseId);

    List<DemandForecast> findByStatus(String status);

    List<DemandForecast> findByPeriod(String period);

    List<DemandForecast> findAllByStatusOrderByGeneratedAtDesc(String status);
}
