package com.commerce_pro_backend.finance.repository;

import com.commerce_pro_backend.finance.entity.FinancialPeriod;
import com.commerce_pro_backend.finance.enums.PeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialPeriodRepository extends JpaRepository<FinancialPeriod, String> {
    Optional<FinancialPeriod> findByFiscalYearAndPeriodNumber(Integer fiscalYear, Integer periodNumber);
    List<FinancialPeriod> findByStatus(PeriodStatus status);
    List<FinancialPeriod> findByFiscalYearOrderByPeriodNumberAsc(Integer fiscalYear);

    @Query("SELECT p FROM FinancialPeriod p WHERE p.status = 'OPEN' ORDER BY p.startDate DESC")
    Optional<FinancialPeriod> findCurrentOpenPeriod();

    @Query("SELECT p FROM FinancialPeriod p WHERE :date BETWEEN p.startDate AND p.endDate AND p.status IN ('OPEN','CLOSING')")
    Optional<FinancialPeriod> findPeriodForDate(@Param("date") LocalDate date);
}
