package com.commerce_pro_backend.supplier.directory.repository;

import com.commerce_pro_backend.supplier.directory.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, String>, JpaSpecificationExecutor<Supplier> {

    Optional<Supplier> findByCode(String code);

    boolean existsByCode(String code);

    List<Supplier> findByIsActiveTrueOrderByNameAsc();

    Page<Supplier> findByIsActiveTrueOrderByNameAsc(Pageable pageable);

    List<Supplier> findByIsPreferredTrueAndIsActiveTrueOrderByNameAsc();

    @Modifying
    @Query("UPDATE Supplier s SET s.isActive = :active WHERE s.id = :id")
    int updateActiveStatus(@Param("id") String id, @Param("active") boolean active);

    @Modifying
    @Query("UPDATE Supplier s SET s.isPreferred = :preferred WHERE s.id = :id")
    int updatePreferredStatus(@Param("id") String id, @Param("preferred") boolean preferred);
}
