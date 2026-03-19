package com.commerce_pro_backend.customer.repository;

import com.commerce_pro_backend.customer.entity.Customer;
import com.commerce_pro_backend.customer.enums.CustomerStatus;
import com.commerce_pro_backend.customer.enums.CustomerTier;
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
public interface CustomerRepository extends JpaRepository<Customer, String>,
        JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByPhone(String phone);
    Optional<Customer> findByReferralCode(String referralCode);
    Optional<Customer> findByLinkedUserId(String linkedUserId);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, String id);
    boolean existsByReferralCode(String referralCode);

    Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);
    Page<Customer> findByTier(CustomerTier tier, Pageable pageable);
    Page<Customer> findByGroupId(String groupId, Pageable pageable);
    Page<Customer> findByIsBlacklistedTrue(Pageable pageable);
    List<Customer> findByTier(CustomerTier tier);
    List<Customer> findByReferredByCustomerId(String referredByCustomerId);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.status = :status")
    long countByStatus(@Param("status") CustomerStatus status);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.tier = :tier")
    long countByTier(@Param("tier") CustomerTier tier);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.createdAt >= :since")
    long countCreatedSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.isBlacklisted = true")
    long countBlacklisted();

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.isFraudFlagged = true AND c.fraudResolved = false")
    long countActiveFraudFlagged();

    @Query("SELECT COALESCE(SUM(c.lifetimeSpend), 0) FROM Customer c")
    BigDecimal sumTotalLifetimeRevenue();

    @Query("SELECT COALESCE(AVG(c.lifetimeSpend), 0) FROM Customer c WHERE c.lifetimeSpend > 0")
    BigDecimal avgLifetimeValue();

    @Query("SELECT COALESCE(SUM(c.loyaltyPoints), 0) FROM Customer c WHERE c.status = 'ACTIVE'")
    long sumActiveLoyaltyPoints();

    @Query("SELECT c.tier, COUNT(c) FROM Customer c GROUP BY c.tier")
    List<Object[]> countGroupedByTier();

    @Query("SELECT c.status, COUNT(c) FROM Customer c GROUP BY c.status")
    List<Object[]> countGroupedByStatus();

    @Query("""
        SELECT c FROM Customer c WHERE
        LOWER(c.firstName) LIKE LOWER(CONCAT('%',:q,'%')) OR
        LOWER(c.lastName)  LIKE LOWER(CONCAT('%',:q,'%')) OR
        LOWER(c.email)     LIKE LOWER(CONCAT('%',:q,'%')) OR
        c.phone            LIKE CONCAT('%',:q,'%')
    """)
    Page<Customer> search(@Param("q") String query, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.status = 'ACTIVE' AND (c.lifetimeSpend >= :spend OR c.totalOrders >= :orders) AND c.tier != :tier")
    List<Customer> findEligibleForTierUpgrade(@Param("tier") CustomerTier tier,
                                               @Param("spend") BigDecimal spend,
                                               @Param("orders") int orders);
}
