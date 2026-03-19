package com.commerce_pro_backend.customer.service;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.customer.dto.WishlistDTO;
import com.commerce_pro_backend.customer.entity.*;
import com.commerce_pro_backend.customer.enums.WishlistVisibility;
import com.commerce_pro_backend.customer.mapper.CustomerMapper;
import com.commerce_pro_backend.customer.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerWishlistService {

    private final WishlistRepository     wishlistRepo;
    private final WishlistItemRepository itemRepo;
    private final CustomerService        customerService;
    private final CustomerMapper         mapper;

    public List<WishlistDTO.Response> getWishlists(String customerId) {
        customerService.findCustomer(customerId);
        return wishlistRepo.findByCustomerId(customerId)
                .stream().map(mapper::toWishlistResponse).collect(Collectors.toList());
    }

    public WishlistDTO.Response getWishlist(String customerId, String wishlistId) {
        return mapper.toWishlistResponse(findWishlist(wishlistId, customerId));
    }

    @Transactional
    public WishlistDTO.Response createWishlist(String customerId, WishlistDTO.Request req) {
        Customer customer = customerService.findCustomer(customerId);
        Wishlist w = Wishlist.builder()
                .customer(customer).name(req.getName())
                .visibility(req.getVisibility() != null ? WishlistVisibility.valueOf(req.getVisibility()) : WishlistVisibility.PRIVATE)
                .isDefault(req.getIsDefault() != null ? req.getIsDefault() : false)
                .build();
        return mapper.toWishlistResponse(wishlistRepo.save(w));
    }

    @Transactional
    public WishlistDTO.Response addItem(String customerId, String wishlistId, WishlistDTO.AddItemRequest req) {
        Wishlist w = findWishlist(wishlistId, customerId);
        if (itemRepo.existsByWishlistIdAndProductId(wishlistId, req.getProductId()))
            throw ApiException.conflict("Product already in this wishlist");

        WishlistItem item = WishlistItem.builder()
                .wishlist(w).productId(req.getProductId())
                .variantInfo(req.getVariantInfo()).note(req.getNote())
                .build();
        itemRepo.save(item);
        return mapper.toWishlistResponse(wishlistRepo.findById(wishlistId).orElseThrow());
    }

    @Transactional
    public void removeItem(String customerId, String wishlistId, String productId) {
        findWishlist(wishlistId, customerId);
        itemRepo.deleteByWishlistIdAndProductId(wishlistId, productId);
    }

    @Transactional
    public WishlistDTO.Response generateShareToken(String customerId, String wishlistId) {
        Wishlist w = findWishlist(wishlistId, customerId);
        if (w.getShareToken() == null) {
            w.setShareToken(UUID.randomUUID().toString());
            w.setVisibility(WishlistVisibility.SHARED);
            wishlistRepo.save(w);
        }
        return mapper.toWishlistResponse(w);
    }

    @Transactional
    public void deleteWishlist(String customerId, String wishlistId) {
        Wishlist w = findWishlist(wishlistId, customerId);
        wishlistRepo.delete(w);
    }

    private Wishlist findWishlist(String wishlistId, String customerId) {
        Wishlist w = wishlistRepo.findById(wishlistId)
                .orElseThrow(() -> ApiException.notFound("Wishlist", wishlistId));
        if (!w.getCustomer().getId().equals(customerId))
            throw ApiException.forbidden("Wishlist does not belong to this customer");
        return w;
    }
}
