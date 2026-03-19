package com.commerce_pro_backend.customer.repository;

import com.commerce_pro_backend.customer.entity.CustomerGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerGroupRepository extends JpaRepository<CustomerGroup, String> {
    Optional<CustomerGroup> findByName(String name);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, String id);
    Page<CustomerGroup> findByIsActiveTrue(Pageable pageable);

    @Modifying
    @Query("UPDATE CustomerGroup g SET g.memberCount = (SELECT COUNT(c) FROM Customer c WHERE c.group.id = :id) WHERE g.id = :id")
    void refreshMemberCount(@Param("id") String id);
}
