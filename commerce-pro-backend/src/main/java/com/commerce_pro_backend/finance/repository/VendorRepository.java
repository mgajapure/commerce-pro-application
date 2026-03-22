package com.commerce_pro_backend.finance.repository;

import com.commerce_pro_backend.finance.entity.Vendor;
import com.commerce_pro_backend.finance.enums.VendorStatus;
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
public interface VendorRepository extends JpaRepository<Vendor, String>, JpaSpecificationExecutor<Vendor> {
    Optional<Vendor> findByVendorCode(String vendorCode);
    boolean existsByEmail(String email);
    List<Vendor> findByStatus(VendorStatus status);
    Page<Vendor> findByStatus(VendorStatus status, Pageable pageable);

    @Query("SELECT v FROM Vendor v WHERE LOWER(v.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(v.email) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<Vendor> search(@Param("q") String query, Pageable pageable);
}
