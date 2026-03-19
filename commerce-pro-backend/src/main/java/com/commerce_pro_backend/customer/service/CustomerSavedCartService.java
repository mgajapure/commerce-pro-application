package com.commerce_pro_backend.customer.service;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.customer.dto.SavedCartDTO;
import com.commerce_pro_backend.customer.entity.*;
import com.commerce_pro_backend.customer.mapper.CustomerMapper;
import com.commerce_pro_backend.customer.repository.SavedCartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerSavedCartService {

    private final SavedCartRepository savedCartRepo;
    private final CustomerService     customerService;
    private final CustomerMapper      mapper;

    public List<SavedCartDTO.Response> getSavedCarts(String customerId) {
        customerService.findCustomer(customerId);
        return savedCartRepo.findByCustomerId(customerId)
                .stream().map(mapper::toSavedCartResponse).collect(Collectors.toList());
    }

    @Transactional
    public SavedCartDTO.Response saveCart(String customerId, SavedCartDTO.Request req) {
        Customer customer = customerService.findCustomer(customerId);

        List<SavedCartItem> items = new ArrayList<>();
        if (req.getItems() != null) {
            for (var itemReq : req.getItems()) {
                items.add(SavedCartItem.builder()
                        .productId(itemReq.getProductId())
                        .quantity(itemReq.getQuantity())
                        .variantInfo(itemReq.getVariantInfo())
                        .build());
            }
        }

        SavedCart cart = SavedCart.builder()
                .customer(customer)
                .cartName(req.getCartName() != null ? req.getCartName() : "Saved Cart")
                .couponCode(req.getCouponCode())
                .estimatedTotal(BigDecimal.ZERO)
                .build();

        items.forEach(i -> i.setSavedCart(cart));
        cart.getItems().addAll(items);

        return mapper.toSavedCartResponse(savedCartRepo.save(cart));
    }

    @Transactional
    public void deleteCart(String customerId, String cartId) {
        customerService.findCustomer(customerId);
        SavedCart cart = savedCartRepo.findById(cartId)
                .orElseThrow(() -> ApiException.notFound("SavedCart", cartId));
        if (!cart.getCustomer().getId().equals(customerId))
            throw ApiException.forbidden("Cart does not belong to this customer");
        savedCartRepo.delete(cart);
    }
}
