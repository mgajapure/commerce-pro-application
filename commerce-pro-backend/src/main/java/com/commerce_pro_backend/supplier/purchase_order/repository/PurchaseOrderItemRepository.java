package com.commerce_pro_backend.supplier.purchase_order.repository;

import com.commerce_pro_backend.supplier.purchase_order.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, String> {

    List<PurchaseOrderItem> findByPurchaseOrderId(String purchaseOrderId);

    void deleteByPurchaseOrderId(String purchaseOrderId);
}
