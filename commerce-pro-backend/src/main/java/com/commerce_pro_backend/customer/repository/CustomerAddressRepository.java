package com.commerce_pro_backend.customer.repository;

import com.commerce_pro_backend.customer.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, String> {
    List<CustomerAddress> findByCustomerId(String customerId);
    Optional<CustomerAddress> findByCustomerIdAndIsDefaultTrue(String customerId);
    long countByCustomerId(String customerId);

    @Modifying
    @Query("UPDATE CustomerAddress a SET a.isDefault = false WHERE a.customer.id = :customerId AND a.id != :excludeId")
    void clearDefaultExcept(@Param("customerId") String customerId, @Param("excludeId") String excludeId);
}
