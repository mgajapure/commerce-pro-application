package com.commerce_pro_backend.catalog.review.repository;

import com.commerce_pro_backend.catalog.review.entity.Review;
import com.commerce_pro_backend.catalog.review.entity.Review.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String>, JpaSpecificationExecutor<Review> {

    Page<Review> findByProductId(String productId, Pageable pageable);

    Page<Review> findByProductIdAndStatus(String productId, ReviewStatus status, Pageable pageable);

    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

    Page<Review> findByRating(Integer rating, Pageable pageable);

    @Modifying
    @Query("UPDATE Review r SET r.status = :status WHERE r.id = :id")
    int updateStatus(@Param("id") String id, @Param("status") ReviewStatus status);

    long countByStatus(ReviewStatus status);

    long countByProductId(String productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.status = 'APPROVED'")
    Double getAverageRating();

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId AND r.status = 'APPROVED'")
    Double getAverageRatingByProductId(@Param("productId") String productId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r GROUP BY r.rating ORDER BY r.rating")
    List<Object[]> getRatingDistribution();

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.productId = :productId AND r.status = 'APPROVED' GROUP BY r.rating ORDER BY r.rating")
    List<Object[]> getRatingDistributionByProductId(@Param("productId") String productId);
}
