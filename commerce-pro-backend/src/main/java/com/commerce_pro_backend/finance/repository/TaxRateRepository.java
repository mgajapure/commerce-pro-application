package com.commerce_pro_backend.finance.repository;

import com.commerce_pro_backend.finance.entity.TaxRate;
import com.commerce_pro_backend.finance.enums.TaxType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, String> {
    Optional<TaxRate> findByRateCode(String rateCode);
    List<TaxRate> findByIsActiveTrue();
    List<TaxRate> findByTaxType(TaxType taxType);
    List<TaxRate> findByCountryAndIsActiveTrue(String country);
    List<TaxRate> findByCountryAndStateProvinceAndIsActiveTrue(String country, String stateProvince);

    @Query("SELECT t FROM TaxRate t WHERE t.isActive = true AND (:date BETWEEN t.effectiveFrom AND t.effectiveTo OR (t.effectiveFrom IS NULL AND t.effectiveTo IS NULL))")
    List<TaxRate> findEffectiveRates(@Param("date") LocalDate date);
}
