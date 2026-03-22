package com.commerce_pro_backend.finance.repository;

import com.commerce_pro_backend.finance.entity.Expense;
import com.commerce_pro_backend.finance.enums.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, String>, JpaSpecificationExecutor<Expense> {
    Optional<Expense> findByExpenseRef(String ref);
    Page<Expense> findByStatus(ExpenseStatus status, Pageable pageable);
    long countByStatus(ExpenseStatus status);

    @Query("SELECT COALESCE(SUM(e.totalAmount), 0) FROM Expense e WHERE e.status NOT IN ('REJECTED','CANCELLED') AND e.expenseDate BETWEEN :from AND :to")
    BigDecimal sumExpensesInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT e.category.id, e.category.name, COALESCE(SUM(e.totalAmount), 0) FROM Expense e WHERE e.status NOT IN ('REJECTED','CANCELLED') AND e.expenseDate BETWEEN :from AND :to GROUP BY e.category.id, e.category.name")
    List<Object[]> sumByCategory(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(e.totalAmount), 0) FROM Expense e WHERE e.period.id = :periodId AND e.status NOT IN ('REJECTED','CANCELLED')")
    BigDecimal sumByPeriod(@Param("periodId") String periodId);
}
