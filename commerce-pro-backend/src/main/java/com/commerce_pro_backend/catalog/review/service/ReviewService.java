package com.commerce_pro_backend.catalog.review.service;

import com.commerce_pro_backend.catalog.review.dto.ReviewDto;
import com.commerce_pro_backend.catalog.review.entity.Review;
import com.commerce_pro_backend.catalog.review.entity.Review.ReviewStatus;
import com.commerce_pro_backend.catalog.review.mapper.ReviewMapper;
import com.commerce_pro_backend.catalog.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;

    public Page<ReviewDto.ListResponse> getAllReviews(String productId, String status, Integer rating, Pageable pageable) {
        if (productId != null && status != null) {
            return reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.valueOf(status), pageable)
                    .map(reviewMapper::toListResponse);
        }
        if (productId != null) {
            return reviewRepository.findByProductId(productId, pageable)
                    .map(reviewMapper::toListResponse);
        }
        if (status != null) {
            return reviewRepository.findByStatus(ReviewStatus.valueOf(status), pageable)
                    .map(reviewMapper::toListResponse);
        }
        if (rating != null) {
            return reviewRepository.findByRating(rating, pageable)
                    .map(reviewMapper::toListResponse);
        }
        return reviewRepository.findAll(pageable)
                .map(reviewMapper::toListResponse);
    }

    public ReviewDto.Response getReview(String id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found: " + id));
        return reviewMapper.toResponse(review);
    }

    public Page<ReviewDto.ListResponse> getReviewsByProductId(String productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable)
                .map(reviewMapper::toListResponse);
    }

    @Transactional
    public ReviewDto.Response createReview(ReviewDto.Request request) {
        log.info("Creating review for product: {} by: {}", request.getProductId(), request.getCustomerName());

        Review review = reviewMapper.toEntity(request);
        Review saved = reviewRepository.save(review);

        log.info("Created review with id: {}", saved.getId());
        return reviewMapper.toResponse(saved);
    }

    @Transactional
    public ReviewDto.Response updateReview(String id, ReviewDto.Request request) {
        log.info("Updating review: {}", id);

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found: " + id));

        reviewMapper.updateEntityFromDto(request, review);
        Review updated = reviewRepository.save(review);
        return reviewMapper.toResponse(updated);
    }

    @Transactional
    public void deleteReview(String id) {
        log.info("Deleting review: {}", id);
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found: " + id));
        reviewRepository.delete(review);
    }

    @Transactional
    public ReviewDto.Response approveReview(String id) {
        log.info("Approving review: {}", id);
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found: " + id));
        review.setStatus(ReviewStatus.APPROVED);
        Review saved = reviewRepository.save(review);
        return reviewMapper.toResponse(saved);
    }

    @Transactional
    public ReviewDto.Response rejectReview(String id) {
        log.info("Rejecting review: {}", id);
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found: " + id));
        review.setStatus(ReviewStatus.REJECTED);
        Review saved = reviewRepository.save(review);
        return reviewMapper.toResponse(saved);
    }

    @Transactional
    public ReviewDto.Response replyToReview(String id, ReviewDto.ReplyRequest request) {
        log.info("Replying to review: {}", id);
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found: " + id));
        review.setReply(request.getReply());
        review.setRepliedBy(request.getRepliedBy());
        review.setRepliedAt(Instant.now());
        Review saved = reviewRepository.save(review);
        return reviewMapper.toResponse(saved);
    }

    public ReviewDto.StatsResponse getStats() {
        long total = reviewRepository.count();
        long pending = reviewRepository.countByStatus(ReviewStatus.PENDING);
        long approved = reviewRepository.countByStatus(ReviewStatus.APPROVED);
        long rejected = reviewRepository.countByStatus(ReviewStatus.REJECTED);
        long flagged = reviewRepository.countByStatus(ReviewStatus.FLAGGED);
        Double avgRating = reviewRepository.getAverageRating();
        List<Object[]> distribution = reviewRepository.getRatingDistribution();

        Map<Integer, Long> ratingDistribution = new HashMap<>();
        for (Object[] row : distribution) {
            ratingDistribution.put((Integer) row[0], (Long) row[1]);
        }

        return ReviewDto.StatsResponse.builder()
                .totalReviews(total)
                .pendingReviews(pending)
                .approvedReviews(approved)
                .rejectedReviews(rejected)
                .flaggedReviews(flagged)
                .averageRating(avgRating != null ? avgRating : 0.0)
                .ratingDistribution(ratingDistribution)
                .build();
    }
}
