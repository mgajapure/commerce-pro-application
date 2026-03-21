package com.commerce_pro_backend.order.repository;

import com.commerce_pro_backend.order.entity.Order;
import com.commerce_pro_backend.order.enums.OrderStatus;
import com.commerce_pro_backend.order.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    List<Order> findByCustomerId(String customerId);

    Page<Order> findByCustomerId(String customerId, Pageable pageable);

    List<Order> findByStatus(OrderStatus status);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    List<Order> findByIsFlaggedTrue();

    @Query("SELECT o FROM Order o WHERE o.status IN :statuses")
    Page<Order> findByStatusIn(@Param("statuses") List<OrderStatus> statuses, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :from AND :to")
    Page<Order> findByCreatedAtBetween(@Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to,
                                       Pageable pageable);

    // ── Dashboard / stats queries ──────────────────────────────────────────────

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.createdAt >= :since")
    long countByStatusSince(@Param("status") OrderStatus status, @Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status NOT IN ('CANCELLED', 'REFUNDED', 'PAYMENT_FAILED')")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status NOT IN ('CANCELLED', 'REFUNDED', 'PAYMENT_FAILED') AND o.createdAt >= :since")
    BigDecimal sumRevenueSince(@Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(AVG(o.totalAmount), 0) FROM Order o WHERE o.status NOT IN ('CANCELLED', 'REFUNDED', 'PAYMENT_FAILED')")
    BigDecimal averageOrderValue();

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.isFlagged = true AND o.status NOT IN ('CANCELLED', 'CLOSED')")
    long countFlaggedOpen();

    @Query("SELECT o FROM Order o WHERE o.customerEmail = :email ORDER BY o.createdAt DESC")
    List<Order> findByCustomerEmail(@Param("email") String email);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :since")
    long countOrdersSince(@Param("since") LocalDateTime since);

    @Query("""
        SELECT o FROM Order o
        WHERE (LOWER(o.orderNumber) LIKE LOWER(CONCAT('%',:q,'%'))
            OR LOWER(o.customerName) LIKE LOWER(CONCAT('%',:q,'%'))
            OR LOWER(o.customerEmail) LIKE LOWER(CONCAT('%',:q,'%')))
        """)
    Page<Order> search(@Param("q") String query, Pageable pageable);

    // ── Analytics queries ────────────────────────────────────────────────────

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :from AND :to AND o.status NOT IN :excluded")
    List<Order> findByCreatedAtBetweenAndStatusNotIn(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("excluded") List<OrderStatus> excluded);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :from AND :to")
    List<Order> findAllByCreatedAtBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Returns Object[]{statusString, count} grouped by status in range */
    @Query("SELECT CAST(o.status AS string), COUNT(o) FROM Order o WHERE o.createdAt BETWEEN :from AND :to GROUP BY o.status")
    List<Object[]> countByStatusInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Returns Object[]{sourceString, count} grouped by source in range */
    @Query("SELECT CAST(o.source AS string), COUNT(o) FROM Order o WHERE o.createdAt BETWEEN :from AND :to GROUP BY o.source")
    List<Object[]> countBySourceInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN :statuses AND o.createdAt BETWEEN :from AND :to")
    long countByStatusInAndCreatedAtBetween(
            @Param("statuses") List<OrderStatus> statuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status NOT IN :excluded AND o.createdAt BETWEEN :from AND :to")
    BigDecimal sumRevenueInRange(
            @Param("excluded") List<OrderStatus> excluded,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(o.taxAmount), 0) FROM Order o WHERE o.status NOT IN :excluded AND o.createdAt BETWEEN :from AND :to")
    BigDecimal sumTaxInRange(
            @Param("excluded") List<OrderStatus> excluded,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
