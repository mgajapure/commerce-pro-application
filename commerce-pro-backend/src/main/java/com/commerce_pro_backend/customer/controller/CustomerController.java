package com.commerce_pro_backend.customer.controller;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.common.dto.PageResponse;
import com.commerce_pro_backend.customer.dto.*;
import com.commerce_pro_backend.customer.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/customers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Customer Management", description = "Full customer lifecycle management API")
public class CustomerController {

    private final CustomerService            customerService;
    private final CustomerAddressService     addressService;
    private final CustomerCommunicationService communicationService;
    private final CustomerWishlistService    wishlistService;
    private final CustomerSavedCartService   savedCartService;
    private final CustomerOrderQueryService  orderQueryService;

    // ── Core CRUD ─────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('customer:customer:read')")
    @Operation(summary = "List customers with filtering and pagination")
    public ResponseEntity<ApiResponse<PageResponse<CustomerSummaryDTO>>> list(
            @ParameterObject @ModelAttribute CustomerFilterDTO filter,
            @PageableDefault(size = 20, sort = "createdAt") @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved", customerService.listCustomers(filter, pageable)));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('customer:customer:stats')")
    @Operation(summary = "Customer statistics dashboard")
    public ResponseEntity<ApiResponse<CustomerStatsDTO>> stats() {
        return ResponseEntity.ok(ApiResponse.success("Stats retrieved", customerService.getStats()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:customer:read')")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Customer retrieved", customerService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('customer:customer:create')")
    @Operation(summary = "Create new customer")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> create(@Valid @RequestBody CustomerRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created", customerService.createCustomer(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:customer:update')")
    @Operation(summary = "Update customer")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> update(@PathVariable String id,
                                                                    @Valid @RequestBody CustomerRequestDTO req) {
        return ResponseEntity.ok(ApiResponse.success("Customer updated", customerService.updateCustomer(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:customer:delete')")
    @Operation(summary = "Delete customer")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted", null));
    }

    // ── Status & Tier ─────────────────────────────────────────────────────

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('customer:customer:manage-status')")
    @Operation(summary = "Set customer status (ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION)")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> setStatus(@PathVariable String id,
                                                                       @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", customerService.setStatus(id, status)));
    }

    @PostMapping("/{id}/evaluate-tier")
    @PreAuthorize("hasAuthority('customer:customer:manage-tier')")
    @Operation(summary = "Re-evaluate and update customer tier based on lifetime stats")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> evaluateTier(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Tier evaluated", customerService.evaluateAndUpdateTier(id)));
    }

    // ── Blacklist & Fraud ─────────────────────────────────────────────────

    @PostMapping("/{id}/blacklist")
    @PreAuthorize("hasAuthority('customer:customer:blacklist')")
    @Operation(summary = "Blacklist a customer")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> blacklist(@PathVariable String id,
                                                                       @Valid @RequestBody BlacklistDTO.BlacklistRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Customer blacklisted", customerService.blacklist(id, req)));
    }

    @PostMapping("/{id}/unblacklist")
    @PreAuthorize("hasAuthority('customer:customer:blacklist')")
    @Operation(summary = "Remove customer from blacklist")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> unblacklist(@PathVariable String id,
                                                                          @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success("Customer removed from blacklist", customerService.unblacklist(id, reason)));
    }

    @PostMapping("/{id}/fraud-flag")
    @PreAuthorize("hasAuthority('customer:customer:fraud-flag')")
    @Operation(summary = "Flag customer for fraud")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> fraudFlag(@PathVariable String id,
                                                                       @Valid @RequestBody BlacklistDTO.FraudFlagRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Customer flagged for fraud", customerService.flagFraud(id, req)));
    }

    @PostMapping("/{id}/fraud-resolve")
    @PreAuthorize("hasAuthority('customer:customer:fraud-flag')")
    @Operation(summary = "Resolve a fraud flag")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> fraudResolve(@PathVariable String id,
                                                                          @Valid @RequestBody BlacklistDTO.FraudResolveRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Fraud flag resolved", customerService.resolveFraud(id, req)));
    }

    // ── Loyalty ───────────────────────────────────────────────────────────

    @PostMapping("/{id}/loyalty/adjust")
    @PreAuthorize("hasAuthority('customer:customer:manage-loyalty')")
    @Operation(summary = "Adjust loyalty points (positive = add, negative = deduct)")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> adjustLoyalty(@PathVariable String id,
                                                                            @Valid @RequestBody LoyaltyAdjustDTO req) {
        return ResponseEntity.ok(ApiResponse.success("Loyalty points adjusted", customerService.adjustLoyaltyPoints(id, req)));
    }

    // ── Tags ──────────────────────────────────────────────────────────────

    @PutMapping("/{id}/tags")
    @PreAuthorize("hasAuthority('customer:customer:update')")
    @Operation(summary = "Replace customer tags")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> updateTags(@PathVariable String id,
                                                                        @RequestBody TagsUpdateDTO req) {
        return ResponseEntity.ok(ApiResponse.success("Tags updated", customerService.updateTags(id, req)));
    }

    // ── Order history ─────────────────────────────────────────────────────

    @GetMapping("/{id}/orders")
    @PreAuthorize("hasAuthority('customer:customer:read')")
    @Operation(summary = "Get order history for a customer")
    public ResponseEntity<ApiResponse<PageResponse<OrderHistoryDTO>>> getOrders(@PathVariable String id,
                                                                                 @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Order history retrieved", orderQueryService.getOrderHistory(id, pageable)));
    }

    // ── Addresses ─────────────────────────────────────────────────────────

    @GetMapping("/{id}/addresses")
    @PreAuthorize("hasAuthority('customer:customer:read')")
    @Operation(summary = "Get address book")
    public ResponseEntity<ApiResponse<?>> getAddresses(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Addresses retrieved", addressService.getAddresses(id)));
    }

    @PostMapping("/{id}/addresses")
    @PreAuthorize("hasAuthority('customer:customer:update')")
    @Operation(summary = "Add address to address book")
    public ResponseEntity<ApiResponse<AddressDTO.Response>> addAddress(@PathVariable String id,
                                                                         @Valid @RequestBody AddressDTO.Request req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address added", addressService.addAddress(id, req)));
    }

    @PutMapping("/{id}/addresses/{addressId}")
    @PreAuthorize("hasAuthority('customer:customer:update')")
    @Operation(summary = "Update an address")
    public ResponseEntity<ApiResponse<AddressDTO.Response>> updateAddress(@PathVariable String id,
                                                                            @PathVariable String addressId,
                                                                            @Valid @RequestBody AddressDTO.Request req) {
        return ResponseEntity.ok(ApiResponse.success("Address updated", addressService.updateAddress(id, addressId, req)));
    }

    @DeleteMapping("/{id}/addresses/{addressId}")
    @PreAuthorize("hasAuthority('customer:customer:update')")
    @Operation(summary = "Delete an address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable String id, @PathVariable String addressId) {
        addressService.deleteAddress(id, addressId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted", null));
    }

    @PostMapping("/{id}/addresses/{addressId}/set-default")
    @PreAuthorize("hasAuthority('customer:customer:update')")
    @Operation(summary = "Set address as default")
    public ResponseEntity<ApiResponse<AddressDTO.Response>> setDefaultAddress(@PathVariable String id,
                                                                                @PathVariable String addressId) {
        return ResponseEntity.ok(ApiResponse.success("Default address set", addressService.setDefault(id, addressId)));
    }

    // ── Communication logs ────────────────────────────────────────────────

    @GetMapping("/{id}/communications")
    @PreAuthorize("hasAuthority('customer:customer:read')")
    @Operation(summary = "Get communication log history")
    public ResponseEntity<ApiResponse<PageResponse<CommunicationLogDTO.Response>>> getCommunications(
            @PathVariable String id, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Communication logs retrieved",
                communicationService.getLogs(id, pageable)));
    }

    @PostMapping("/{id}/communications")
    @PreAuthorize("hasAuthority('customer:communication:create')")
    @Operation(summary = "Add a communication log entry")
    public ResponseEntity<ApiResponse<CommunicationLogDTO.Response>> addCommunication(
            @PathVariable String id, @Valid @RequestBody CommunicationLogDTO.Request req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Communication logged", communicationService.addLog(id, req)));
    }

    @PostMapping("/{id}/communications/{logId}/resolve")
    @PreAuthorize("hasAuthority('customer:communication:create')")
    @Operation(summary = "Mark communication as resolved")
    public ResponseEntity<ApiResponse<CommunicationLogDTO.Response>> resolveCommunication(
            @PathVariable String id, @PathVariable String logId) {
        return ResponseEntity.ok(ApiResponse.success("Communication resolved", communicationService.resolve(id, logId)));
    }

    // ── Wishlists ─────────────────────────────────────────────────────────

    @GetMapping("/{id}/wishlists")
    @PreAuthorize("hasAuthority('customer:customer:read')")
    @Operation(summary = "Get all wishlists")
    public ResponseEntity<ApiResponse<?>> getWishlists(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Wishlists retrieved", wishlistService.getWishlists(id)));
    }

    @PostMapping("/{id}/wishlists")
    @PreAuthorize("hasAuthority('customer:customer:update')")
    @Operation(summary = "Create wishlist")
    public ResponseEntity<ApiResponse<WishlistDTO.Response>> createWishlist(@PathVariable String id,
                                                                              @Valid @RequestBody WishlistDTO.Request req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Wishlist created", wishlistService.createWishlist(id, req)));
    }

    @PostMapping("/{id}/wishlists/{wishlistId}/items")
    @PreAuthorize("hasAuthority('customer:customer:update')")
    @Operation(summary = "Add product to wishlist")
    public ResponseEntity<ApiResponse<WishlistDTO.Response>> addToWishlist(@PathVariable String id,
                                                                             @PathVariable String wishlistId,
                                                                             @Valid @RequestBody WishlistDTO.AddItemRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Item added to wishlist", wishlistService.addItem(id, wishlistId, req)));
    }

    @DeleteMapping("/{id}/wishlists/{wishlistId}/items/{productId}")
    @PreAuthorize("hasAuthority('customer:customer:update')")
    @Operation(summary = "Remove product from wishlist")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(@PathVariable String id,
                                                                  @PathVariable String wishlistId,
                                                                  @PathVariable String productId) {
        wishlistService.removeItem(id, wishlistId, productId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from wishlist", null));
    }

    @PostMapping("/{id}/wishlists/{wishlistId}/share")
    @PreAuthorize("hasAuthority('customer:customer:update')")
    @Operation(summary = "Generate share token for wishlist")
    public ResponseEntity<ApiResponse<WishlistDTO.Response>> shareWishlist(@PathVariable String id,
                                                                             @PathVariable String wishlistId) {
        return ResponseEntity.ok(ApiResponse.success("Share token generated", wishlistService.generateShareToken(id, wishlistId)));
    }

    @DeleteMapping("/{id}/wishlists/{wishlistId}")
    @PreAuthorize("hasAuthority('customer:customer:update')")
    @Operation(summary = "Delete wishlist")
    public ResponseEntity<ApiResponse<Void>> deleteWishlist(@PathVariable String id, @PathVariable String wishlistId) {
        wishlistService.deleteWishlist(id, wishlistId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist deleted", null));
    }

    // ── Saved carts ───────────────────────────────────────────────────────

    @GetMapping("/{id}/saved-carts")
    @PreAuthorize("hasAuthority('customer:customer:read')")
    @Operation(summary = "Get saved carts")
    public ResponseEntity<ApiResponse<?>> getSavedCarts(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Saved carts retrieved", savedCartService.getSavedCarts(id)));
    }

    @PostMapping("/{id}/saved-carts")
    @PreAuthorize("hasAuthority('customer:customer:update')")
    @Operation(summary = "Save a cart snapshot")
    public ResponseEntity<ApiResponse<SavedCartDTO.Response>> saveCart(@PathVariable String id,
                                                                         @Valid @RequestBody SavedCartDTO.Request req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cart saved", savedCartService.saveCart(id, req)));
    }

    @DeleteMapping("/{id}/saved-carts/{cartId}")
    @PreAuthorize("hasAuthority('customer:customer:update')")
    @Operation(summary = "Delete a saved cart")
    public ResponseEntity<ApiResponse<Void>> deleteCart(@PathVariable String id, @PathVariable String cartId) {
        savedCartService.deleteCart(id, cartId);
        return ResponseEntity.ok(ApiResponse.success("Cart deleted", null));
    }
}
