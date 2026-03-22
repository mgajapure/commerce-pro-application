package com.commerce_pro_backend.catalog.review.mapper;

import com.commerce_pro_backend.catalog.review.dto.ReviewDto;
import com.commerce_pro_backend.catalog.review.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public Review toEntity(ReviewDto.Request dto) {
        if (dto == null) return null;

        return Review.builder()
                .productId(dto.getProductId())
                .productName(dto.getProductName())
                .customerName(dto.getCustomerName())
                .customerEmail(dto.getCustomerEmail())
                .rating(dto.getRating())
                .title(dto.getTitle())
                .content(dto.getContent())
                .isVerifiedPurchase(dto.getIsVerifiedPurchase() != null ? dto.getIsVerifiedPurchase() : false)
                .build();
    }

    public ReviewDto.Response toResponse(Review entity) {
        if (entity == null) return null;

        return ReviewDto.Response.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .productName(entity.getProductName())
                .customerName(entity.getCustomerName())
                .customerEmail(entity.getCustomerEmail())
                .rating(entity.getRating())
                .title(entity.getTitle())
                .content(entity.getContent())
                .status(entity.getStatus().name())
                .isVerifiedPurchase(entity.getIsVerifiedPurchase())
                .helpfulCount(entity.getHelpfulCount())
                .reportCount(entity.getReportCount())
                .reply(entity.getReply())
                .repliedBy(entity.getRepliedBy())
                .repliedAt(entity.getRepliedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ReviewDto.ListResponse toListResponse(Review entity) {
        if (entity == null) return null;

        return ReviewDto.ListResponse.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .productName(entity.getProductName())
                .customerName(entity.getCustomerName())
                .rating(entity.getRating())
                .title(entity.getTitle())
                .status(entity.getStatus().name())
                .isVerifiedPurchase(entity.getIsVerifiedPurchase())
                .helpfulCount(entity.getHelpfulCount())
                .reportCount(entity.getReportCount())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public void updateEntityFromDto(ReviewDto.Request dto, Review entity) {
        if (dto == null || entity == null) return;

        if (dto.getProductId() != null) entity.setProductId(dto.getProductId());
        if (dto.getProductName() != null) entity.setProductName(dto.getProductName());
        if (dto.getCustomerName() != null) entity.setCustomerName(dto.getCustomerName());
        if (dto.getCustomerEmail() != null) entity.setCustomerEmail(dto.getCustomerEmail());
        if (dto.getRating() != null) entity.setRating(dto.getRating());
        if (dto.getTitle() != null) entity.setTitle(dto.getTitle());
        if (dto.getContent() != null) entity.setContent(dto.getContent());
        if (dto.getIsVerifiedPurchase() != null) entity.setIsVerifiedPurchase(dto.getIsVerifiedPurchase());
    }
}
