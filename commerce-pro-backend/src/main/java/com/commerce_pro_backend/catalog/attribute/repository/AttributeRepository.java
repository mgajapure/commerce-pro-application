package com.commerce_pro_backend.catalog.attribute.repository;

import com.commerce_pro_backend.catalog.attribute.entity.Attribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttributeRepository extends JpaRepository<Attribute, String>, JpaSpecificationExecutor<Attribute> {

    Optional<Attribute> findByCode(String code);

    boolean existsByCode(String code);

    List<Attribute> findByIsActiveTrueOrderBySortOrderAsc();

    @Modifying
    @Query("UPDATE Attribute a SET a.isActive = :active WHERE a.id = :id")
    int updateActiveStatus(@Param("id") String id, @Param("active") boolean active);
}
