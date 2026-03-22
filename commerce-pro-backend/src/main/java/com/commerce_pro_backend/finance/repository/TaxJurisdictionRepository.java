package com.commerce_pro_backend.finance.repository;

import com.commerce_pro_backend.finance.entity.TaxJurisdiction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaxJurisdictionRepository extends JpaRepository<TaxJurisdiction, String> {
    Optional<TaxJurisdiction> findByJurisdictionCode(String code);
    List<TaxJurisdiction> findByIsActiveTrue();
    List<TaxJurisdiction> findByCountry(String country);
}
