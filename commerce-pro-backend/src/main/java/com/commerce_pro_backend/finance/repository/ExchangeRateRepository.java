package com.commerce_pro_backend.finance.repository;

import com.commerce_pro_backend.finance.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, String> {
    Optional<ExchangeRate> findByBaseCurrencyAndQuoteCurrencyAndRateDate(String base, String quote, LocalDate date);

    @Query("SELECT e FROM ExchangeRate e WHERE e.baseCurrency = :base AND e.quoteCurrency = :quote ORDER BY e.rateDate DESC")
    List<ExchangeRate> findLatestRate(@Param("base") String base, @Param("quote") String quote);

    List<ExchangeRate> findByRateDate(LocalDate date);
    boolean existsByBaseCurrencyAndQuoteCurrencyAndRateDate(String base, String quote, LocalDate date);
}
