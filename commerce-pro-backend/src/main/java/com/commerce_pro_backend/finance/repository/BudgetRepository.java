package com.commerce_pro_backend.finance.repository;

import com.commerce_pro_backend.finance.entity.Budget;
import com.commerce_pro_backend.finance.enums.BudgetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, String> {
    List<Budget> findByFiscalYear(Integer fiscalYear);
    Optional<Budget> findByFiscalYearAndStatus(Integer fiscalYear, BudgetStatus status);
    Page<Budget> findByFiscalYear(Integer fiscalYear, Pageable pageable);
}
