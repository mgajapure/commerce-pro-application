package com.commerce_pro_backend.payment.repository;

import com.commerce_pro_backend.payment.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, String> {

    List<PaymentMethod> findByCustomerIdAndIsActiveTrue(String customerId);

    Optional<PaymentMethod> findByCustomerIdAndIsDefaultTrueAndIsActiveTrue(String customerId);

    long countByCustomerIdAndIsActiveTrue(String customerId);
}
