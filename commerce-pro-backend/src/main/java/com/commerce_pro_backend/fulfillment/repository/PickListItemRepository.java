package com.commerce_pro_backend.fulfillment.repository;

import com.commerce_pro_backend.fulfillment.entity.PickListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PickListItemRepository extends JpaRepository<PickListItem, String> {

    List<PickListItem> findByPickListId(String pickListId);

    List<PickListItem> findByOrderId(String orderId);
}
