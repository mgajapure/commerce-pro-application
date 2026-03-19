package com.commerce_pro_backend.customer.mapper;

import com.commerce_pro_backend.customer.dto.*;
import com.commerce_pro_backend.customer.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CustomerMapper {

    public CustomerResponseDTO toResponseDTO(Customer c) {
        if (c == null) return null;
        return CustomerResponseDTO.builder()
                .id(c.getId()).firstName(c.getFirstName()).lastName(c.getLastName())
                .fullName(c.getFullName()).email(c.getEmail()).phone(c.getPhone())
                .secondaryPhone(c.getSecondaryPhone()).dateOfBirth(c.getDateOfBirth())
                .gender(c.getGender()).avatarUrl(c.getAvatarUrl())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .tier(c.getTier() != null ? c.getTier().name() : null)
                .groupId(c.getGroup() != null ? c.getGroup().getId() : null)
                .groupName(c.getGroup() != null ? c.getGroup().getName() : null)
                .lifetimeSpend(c.getLifetimeSpend()).totalOrders(c.getTotalOrders())
                .totalReturns(c.getTotalReturns()).averageOrderValue(c.getAverageOrderValue())
                .lastOrderAt(c.getLastOrderAt()).lastOrderId(c.getLastOrderId())
                .loyaltyPoints(c.getLoyaltyPoints()).taxId(c.getTaxId()).companyName(c.getCompanyName())
                .preferredCurrency(c.getPreferredCurrency()).preferredLanguage(c.getPreferredLanguage())
                .marketingOptIn(c.getMarketingOptIn()).smsOptIn(c.getSmsOptIn())
                .emailVerified(c.getEmailVerified()).phoneVerified(c.getPhoneVerified())
                .acquisitionSource(c.getAcquisitionSource()).referralCode(c.getReferralCode())
                .referredByCustomerId(c.getReferredByCustomerId()).linkedUserId(c.getLinkedUserId())
                .tags(c.getTags()).internalNotes(c.getInternalNotes())
                .isBlacklisted(c.getIsBlacklisted()).blacklistReason(c.getBlacklistReason())
                .blacklistedAt(c.getBlacklistedAt()).isFraudFlagged(c.getIsFraudFlagged())
                .fraudReason(c.getFraudReason()).fraudEvidence(c.getFraudEvidence())
                .fraudFlaggedAt(c.getFraudFlaggedAt()).fraudResolved(c.getFraudResolved())
                .fraudResolution(c.getFraudResolution()).riskScore(c.getRiskScore())
                .addressCount(c.getAddresses() != null ? c.getAddresses().size() : 0)
                .wishlistCount(c.getWishlists() != null ? c.getWishlists().size() : 0)
                .communicationLogCount(c.getCommunicationLogs() != null ? c.getCommunicationLogs().size() : 0)
                .createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt())
                .createdBy(c.getCreatedBy()).updatedBy(c.getUpdatedBy())
                .build();
    }

    public CustomerSummaryDTO toSummaryDTO(Customer c) {
        if (c == null) return null;
        return CustomerSummaryDTO.builder()
                .id(c.getId()).firstName(c.getFirstName()).lastName(c.getLastName())
                .fullName(c.getFullName()).email(c.getEmail()).phone(c.getPhone())
                .avatarUrl(c.getAvatarUrl())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .tier(c.getTier() != null ? c.getTier().name() : null)
                .groupName(c.getGroup() != null ? c.getGroup().getName() : null)
                .lifetimeSpend(c.getLifetimeSpend()).totalOrders(c.getTotalOrders())
                .loyaltyPoints(c.getLoyaltyPoints()).riskScore(c.getRiskScore())
                .isBlacklisted(c.getIsBlacklisted()).isFraudFlagged(c.getIsFraudFlagged())
                .tags(c.getTags()).lastOrderAt(c.getLastOrderAt()).createdAt(c.getCreatedAt())
                .build();
    }

    public List<CustomerSummaryDTO> toSummaryDTOList(List<Customer> list) {
        return list == null ? List.of() : list.stream().map(this::toSummaryDTO).collect(Collectors.toList());
    }

    public AddressDTO.Response toAddressResponse(CustomerAddress a) {
        if (a == null) return null;
        return AddressDTO.Response.builder()
                .id(a.getId())
                .addressLabel(a.getAddressLabel())
                .addressType(a.getAddressType() != null ? a.getAddressType().name() : null)
                .fullName(a.getFullName()).phone(a.getPhone()).company(a.getCompany())
                .addressLine1(a.getAddressLine1()).addressLine2(a.getAddressLine2())
                .city(a.getCity()).state(a.getState()).postalCode(a.getPostalCode()).country(a.getCountry())
                .isDefault(a.getIsDefault()).isVerified(a.getIsVerified()).createdAt(a.getCreatedAt())
                .build();
    }

    public GroupDTO.Response toGroupResponse(CustomerGroup g) {
        if (g == null) return null;
        return GroupDTO.Response.builder()
                .id(g.getId()).name(g.getName()).description(g.getDescription())
                .colorHex(g.getColorHex()).isActive(g.getIsActive())
                .discountPercentage(g.getDiscountPercentage()).internalNotes(g.getInternalNotes())
                .memberCount(g.getMemberCount()).createdAt(g.getCreatedAt()).updatedAt(g.getUpdatedAt())
                .build();
    }

    public CommunicationLogDTO.Response toCommLogResponse(CustomerCommunicationLog l) {
        if (l == null) return null;
        return CommunicationLogDTO.Response.builder()
                .id(l.getId())
                .channel(l.getChannel() != null ? l.getChannel().name() : null)
                .direction(l.getDirection() != null ? l.getDirection().name() : null)
                .subject(l.getSubject()).body(l.getBody())
                .orderId(l.getOrderId()).orderNumber(l.getOrderNumber())
                .loggedBy(l.getLoggedBy()).loggedByName(l.getLoggedByName())
                .loggedAt(l.getLoggedAt()).requiresFollowUp(l.getRequiresFollowUp())
                .followUpAt(l.getFollowUpAt()).isResolved(l.getIsResolved()).createdAt(l.getCreatedAt())
                .build();
    }

    public WishlistDTO.Response toWishlistResponse(Wishlist w) {
        if (w == null) return null;
        var items = w.getItems() == null ? List.<WishlistDTO.ItemResponse>of() :
            w.getItems().stream().map(i -> WishlistDTO.ItemResponse.builder()
                .id(i.getId()).productId(i.getProductId()).productName(i.getProductName())
                .productSku(i.getProductSku()).productImageUrl(i.getProductImageUrl())
                .priceAtAdd(i.getPriceAtAdd()).variantInfo(i.getVariantInfo())
                .note(i.getNote()).addedAt(i.getAddedAt()).build()
            ).collect(Collectors.toList());
        return WishlistDTO.Response.builder()
                .id(w.getId()).name(w.getName())
                .visibility(w.getVisibility() != null ? w.getVisibility().name() : null)
                .shareToken(w.getShareToken()).isDefault(w.getIsDefault())
                .itemCount(items.size()).items(items)
                .createdAt(w.getCreatedAt()).updatedAt(w.getUpdatedAt())
                .build();
    }

    public SavedCartDTO.Response toSavedCartResponse(SavedCart sc) {
        if (sc == null) return null;
        var items = sc.getItems() == null ? List.<SavedCartDTO.ItemResponse>of() :
            sc.getItems().stream().map(i -> SavedCartDTO.ItemResponse.builder()
                .id(i.getId()).productId(i.getProductId()).productName(i.getProductName())
                .productSku(i.getProductSku()).productImageUrl(i.getProductImageUrl())
                .unitPrice(i.getUnitPrice()).quantity(i.getQuantity())
                .variantInfo(i.getVariantInfo()).addedAt(i.getAddedAt()).build()
            ).collect(Collectors.toList());
        return SavedCartDTO.Response.builder()
                .id(sc.getId()).cartName(sc.getCartName()).couponCode(sc.getCouponCode())
                .estimatedTotal(sc.getEstimatedTotal()).isAbandoned(sc.getIsAbandoned())
                .abandonedAt(sc.getAbandonedAt()).recoveryEmailSent(sc.getRecoveryEmailSent())
                .items(items).createdAt(sc.getCreatedAt()).updatedAt(sc.getUpdatedAt())
                .build();
    }
}
