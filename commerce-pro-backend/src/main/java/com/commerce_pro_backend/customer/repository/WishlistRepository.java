package com.commerce_pro_backend.customer.repository;

import com.commerce_pro_backend.customer.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, String> {
    List<Wishlist> findByCustomerId(String customerId);
    Optional<Wishlist> findByCustomerIdAndIsDefaultTrue(String customerId);
    Optional<Wishlist> findByShareToken(String shareToken);
    boolean existsByCustomerIdAndName(String customerId, String name);
}
