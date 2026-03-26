package com.commerce_pro_backend.supplier.product.repository;

import com.commerce_pro_backend.supplier.product.entity.SupplierProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierProductRepository extends JpaRepository<SupplierProduct, String>, JpaSpecificationExecutor<SupplierProduct> {

    List<SupplierProduct> findBySupplierId(String supplierId);

    List<SupplierProduct> findByProductId(String productId);

    Optional<SupplierProduct> findBySupplierIdAndProductId(String supplierId, String productId);

    boolean existsBySupplierIdAndProductId(String supplierId, String productId);

    Page<SupplierProduct> findByIsActiveTrueOrderBySupplierNameAsc(Pageable pageable);

    long countBySupplierId(String supplierId);
}
