package com.commerce_pro_backend.finance.repository;

import com.commerce_pro_backend.finance.entity.CustomerInvoice;
import com.commerce_pro_backend.finance.enums.InvoiceStatus;
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
public interface CustomerInvoiceRepository extends JpaRepository<CustomerInvoice, String>, JpaSpecificationExecutor<CustomerInvoice> {
    Optional<CustomerInvoice> findByInvoiceNumber(String invoiceNumber);
    List<CustomerInvoice> findByOrderId(String orderId);
    Page<CustomerInvoice> findByCustomerId(String customerId, Pageable pageable);
    Page<CustomerInvoice> findByStatus(InvoiceStatus status, Pageable pageable);
    long countByStatus(InvoiceStatus status);

    @Query("SELECT COALESCE(SUM(i.balanceDue), 0) FROM CustomerInvoice i WHERE i.status NOT IN ('PAID','VOID','WRITTEN_OFF')")
    BigDecimal sumTotalOutstanding();

    @Query("SELECT COALESCE(SUM(i.balanceDue), 0) FROM CustomerInvoice i WHERE i.status NOT IN ('PAID','VOID','WRITTEN_OFF') AND i.dueDate < :today")
    BigDecimal sumOverdueAmount(@Param("today") LocalDate today);

    @Query("SELECT i FROM CustomerInvoice i WHERE i.status NOT IN ('PAID','VOID','WRITTEN_OFF') AND i.dueDate < :today ORDER BY i.dueDate ASC")
    List<CustomerInvoice> findOverdueInvoices(@Param("today") LocalDate today);

    @Query("SELECT i FROM CustomerInvoice i WHERE i.customerId = :customerId AND i.status NOT IN ('PAID','VOID','WRITTEN_OFF')")
    List<CustomerInvoice> findOutstandingByCustomer(@Param("customerId") String customerId);

    // AR Aging buckets
    @Query("SELECT COALESCE(SUM(i.balanceDue), 0) FROM CustomerInvoice i WHERE i.status NOT IN ('PAID','VOID','WRITTEN_OFF') AND i.dueDate >= :today")
    BigDecimal sumCurrentBucket(@Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(i.balanceDue), 0) FROM CustomerInvoice i WHERE i.status NOT IN ('PAID','VOID','WRITTEN_OFF') AND i.dueDate < :from AND i.dueDate >= :to")
    BigDecimal sumAgingBucket(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
