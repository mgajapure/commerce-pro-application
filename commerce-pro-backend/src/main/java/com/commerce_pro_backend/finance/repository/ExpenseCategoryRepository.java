package com.commerce_pro_backend.finance.repository;

import com.commerce_pro_backend.finance.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, String> {
    Optional<ExpenseCategory> findByCategoryCode(String code);
    List<ExpenseCategory> findByIsActiveTrue();
    List<ExpenseCategory> findByParentId(String parentId);
}
