package com.commerce_pro_backend.finance.repository;

import com.commerce_pro_backend.finance.entity.VendorInvoice;
import com.commerce_pro_backend.finance.enums.VendorInvoiceStatus;
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
public interface VendorInvoiceRepository extends JpaRepository<VendorInvoice, String>, JpaSpecificationExecutor<VendorInvoice> {
    Optional<VendorInvoice> findByVendorInvoiceNumber(String number);
    Page<VendorInvoice> findByVendorId(String vendorId, Pageable pageable);
    Page<VendorInvoice> findByStatus(VendorInvoiceStatus status, Pageable pageable);
    long countByStatus(VendorInvoiceStatus status);

    @Query("SELECT COALESCE(SUM(v.balanceDue), 0) FROM VendorInvoice v WHERE v.status NOT IN ('PAID','VOID')")
    BigDecimal sumTotalOutstanding();

    @Query("SELECT COALESCE(SUM(v.balanceDue), 0) FROM VendorInvoice v WHERE v.status NOT IN ('PAID','VOID') AND v.dueDate < :today")
    BigDecimal sumOverdueAmount(@Param("today") LocalDate today);

    @Query("SELECT v FROM VendorInvoice v WHERE v.status NOT IN ('PAID','VOID') AND v.dueDate BETWEEN :from AND :to ORDER BY v.dueDate ASC")
    List<VendorInvoice> findDueSoon(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT v FROM VendorInvoice v WHERE v.status NOT IN ('PAID','VOID') AND v.dueDate < :today ORDER BY v.dueDate ASC")
    List<VendorInvoice> findOverdue(@Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(v.balanceDue), 0) FROM VendorInvoice v WHERE v.status NOT IN ('PAID','VOID') AND v.dueDate >= :today")
    BigDecimal sumCurrentBucket(@Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(v.balanceDue), 0) FROM VendorInvoice v WHERE v.status NOT IN ('PAID','VOID') AND v.dueDate < :from AND v.dueDate >= :to")
    BigDecimal sumAgingBucket(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
