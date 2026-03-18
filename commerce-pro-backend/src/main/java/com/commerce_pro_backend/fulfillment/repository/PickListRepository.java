package com.commerce_pro_backend.fulfillment.repository;

import com.commerce_pro_backend.fulfillment.entity.PickList;
import com.commerce_pro_backend.fulfillment.enums.PickListStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PickListRepository extends JpaRepository<PickList, String>, JpaSpecificationExecutor<PickList> {

    Optional<PickList> findByPickListNumber(String pickListNumber);

    Page<PickList> findByStatus(PickListStatus status, Pageable pageable);

    List<PickList> findByAssignedToUserId(String userId);

    @Query("SELECT COUNT(p) FROM PickList p WHERE p.status = :status")
    long countByStatus(@Param("status") PickListStatus status);

    @Query("SELECT p.status, COUNT(p) FROM PickList p GROUP BY p.status")
    List<Object[]> countGroupedByStatus();
}
