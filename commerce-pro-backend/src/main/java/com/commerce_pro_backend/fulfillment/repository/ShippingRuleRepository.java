package com.commerce_pro_backend.fulfillment.repository;

import com.commerce_pro_backend.fulfillment.entity.ShippingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShippingRuleRepository extends JpaRepository<ShippingRule, String> {

    List<ShippingRule> findByIsActiveTrueOrderByPriorityAsc();

    List<ShippingRule> findByCarrierIdOrderByPriorityAsc(String carrierId);
}
