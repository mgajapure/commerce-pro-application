package com.commerce_pro_backend.supplier.contract.repository;

import com.commerce_pro_backend.supplier.contract.entity.SupplierContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierContractRepository extends JpaRepository<SupplierContract, String>, JpaSpecificationExecutor<SupplierContract> {

    Optional<SupplierContract> findByContractNumber(String contractNumber);

    boolean existsByContractNumber(String contractNumber);

    List<SupplierContract> findBySupplierIdOrderByStartDateDesc(String supplierId);

    List<SupplierContract> findByStatus(String status);

    Page<SupplierContract> findByStatusOrderByStartDateDesc(String status, Pageable pageable);

    List<SupplierContract> findByEndDateBeforeAndStatusNot(Instant endDate, String status);

    long countByStatus(String status);
}
