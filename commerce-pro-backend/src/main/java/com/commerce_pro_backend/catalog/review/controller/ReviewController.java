package com.commerce_pro_backend.catalog.review.controller;

import com.commerce_pro_backend.catalog.review.dto.ReviewDto;
import com.commerce_pro_backend.catalog.review.service.ReviewService;
import com.commerce_pro_backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReviewDto.ListResponse>>> getAll(
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer rating,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getAllReviews(productId, status, rating, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewDto.Response>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getReview(id)));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<Page<ReviewDto.ListResponse>>> getByProductId(
            @PathVariable String productId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getReviewsByProductId(productId, pageable)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewDto.Response>> create(
            @Valid @RequestBody ReviewDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(reviewService.createReview(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewDto.Response>> update(
            @PathVariable String id,
            @Valid @RequestBody ReviewDto.Request request) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.updateReview(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable String id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully"));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ReviewDto.Response>> approve(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.approveReview(id)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ReviewDto.Response>> reject(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.rejectReview(id)));
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<ApiResponse<ReviewDto.Response>> reply(
            @PathVariable String id,
            @Valid @RequestBody ReviewDto.ReplyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.replyToReview(id, request)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ReviewDto.StatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getStats()));
    }
}
