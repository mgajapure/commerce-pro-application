package com.commerce_pro_backend.common.data;

import com.commerce_pro_backend.catalog.product.dto.ProductSummaryDTO;
import com.commerce_pro_backend.catalog.product.service.ProductService;
import com.commerce_pro_backend.customer.dto.*;
import com.commerce_pro_backend.customer.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CustomerDataSeeder {

    private final SeedDataLoader seedDataLoader;

    @Bean
    @Profile("dev")
    @Order(3)
    CommandLineRunner seedCustomers(
            CustomerService customerService,
            CustomerGroupService groupService,
            CustomerAddressService addressService,
            CustomerCommunicationService commService,
            CustomerWishlistService wishlistService,
            CustomerSavedCartService savedCartService,
            ProductService productService,
            UserDetailsService userDetailsService) {
        return args -> {
            log.info("━━━ [Seed] Customers ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            try {
                seedDataLoader.authenticateAsSuperAdmin(userDetailsService);

                // Groups
                JsonNode groupsJson = seedDataLoader.loadTree("seed/customer-groups.json");
                List<GroupDTO.Response> groups = new ArrayList<>();
                for (JsonNode g : groupsJson) {
                    var builder = GroupDTO.Request.builder()
                            .name(g.get("name").asText())
                            .description(g.get("description").asText())
                            .colorHex(g.get("colorHex").asText())
                            .isActive(g.get("isActive").asBoolean());
                    if (g.has("discountPercentage")) {
                        builder.discountPercentage(new BigDecimal(g.get("discountPercentage").asText()));
                    }
                    groups.add(groupService.createGroup(builder.build()));
                }
                log.info("[Seed] Created {} customer groups", groups.size());

                // Customers
                JsonNode customersJson = seedDataLoader.loadTree("seed/customers.json");
                List<CustomerResponseDTO> dbCustomers = new ArrayList<>();

                for (JsonNode c : customersJson) {
                    var builder = CustomerRequestDTO.builder()
                            .firstName(c.get("firstName").asText())
                            .lastName(c.get("lastName").asText())
                            .email(c.get("email").asText())
                            .phone(c.get("phone").asText())
                            .dateOfBirth(LocalDate.parse(c.get("dateOfBirth").asText()))
                            .gender(c.get("gender").asText())
                            .acquisitionSource(c.get("acquisitionSource").asText())
                            .marketingOptIn(c.path("marketingOptIn").asBoolean(false))
                            .preferredCurrency(c.get("preferredCurrency").asText())
                            .preferredLanguage(c.get("preferredLanguage").asText());

                    if (c.has("secondaryPhone")) builder.secondaryPhone(c.get("secondaryPhone").asText());
                    if (c.has("companyName")) builder.companyName(c.get("companyName").asText());
                    if (c.has("taxId")) builder.taxId(c.get("taxId").asText());
                    if (c.has("smsOptIn")) builder.smsOptIn(c.get("smsOptIn").asBoolean());
                    if (c.has("internalNotes")) builder.internalNotes(c.get("internalNotes").asText());

                    if (c.has("groupRef")) {
                        String groupName = c.get("groupRef").asText();
                        groups.stream()
                                .filter(g -> g.getName().equals(groupName))
                                .findFirst()
                                .ifPresent(g -> builder.groupId(g.getId()));
                    }

                    dbCustomers.add(customerService.createCustomer(builder.build()));
                }
                log.info("[Seed] Created {} customers with enriched profiles", dbCustomers.size());

                // Tags & Loyalty
                for (int i = 0; i < customersJson.size(); i++) {
                    JsonNode c = customersJson.get(i);
                    String custId = dbCustomers.get(i).getId();

                    if (c.has("tags")) {
                        Set<String> tags = new HashSet<>();
                        c.get("tags").forEach(t -> tags.add(t.asText()));
                        customerService.updateTags(custId,
                                TagsUpdateDTO.builder().tags(tags).build());
                    }
                    if (c.has("loyaltyPoints")) {
                        customerService.adjustLoyaltyPoints(custId,
                                LoyaltyAdjustDTO.builder()
                                        .points(c.get("loyaltyPoints").asInt())
                                        .description(c.get("loyaltyDescription").asText())
                                        .build());
                    }
                }
                log.info("[Seed] Tags and loyalty points assigned");

                // Status overrides
                for (int i = 0; i < customersJson.size(); i++) {
                    JsonNode c = customersJson.get(i);
                    String custId = dbCustomers.get(i).getId();

                    if (c.has("statusOverride")) {
                        customerService.setStatus(custId, c.get("statusOverride").asText());
                    }
                    if (c.has("blacklist")) {
                        customerService.blacklist(custId,
                                BlacklistDTO.BlacklistRequest.builder()
                                        .reason(c.get("blacklist").get("reason").asText())
                                        .build());
                    }
                    if (c.has("fraudFlag")) {
                        customerService.flagFraud(custId,
                                BlacklistDTO.FraudFlagRequest.builder()
                                        .reason(c.get("fraudFlag").get("reason").asText())
                                        .evidence(c.get("fraudFlag").get("evidence").asText())
                                        .build());
                        if (c.has("fraudResolve")) {
                            customerService.resolveFraud(custId,
                                    BlacklistDTO.FraudResolveRequest.builder()
                                            .resolution(c.get("fraudResolve").get("resolution").asText())
                                            .build());
                        }
                    }
                }
                log.info("[Seed] Status variety applied");

                // Addresses
                seedAddresses(addressService, dbCustomers);
                log.info("[Seed] Addresses seeded for all customers");

                // Communication logs
                seedCommunicationLogs(commService, dbCustomers);
                log.info("[Seed] Communication logs seeded");

                // Wishlists
                List<ProductSummaryDTO> products = productService.getAllProducts();
                seedWishlists(wishlistService, dbCustomers, products);
                log.info("[Seed] Wishlists seeded");

                // Saved carts
                seedSavedCarts(savedCartService, dbCustomers, products);
                log.info("[Seed] Saved carts seeded");

                log.info("[Seed] Customers complete — {} created with full data", dbCustomers.size());

            } catch (Exception e) {
                log.error("[Seed] Customer seed failed: {}", e.getMessage(), e);
            } finally {
                seedDataLoader.clearSecurityContext();
            }
        };
    }

    private void seedAddresses(CustomerAddressService addressService,
                               List<CustomerResponseDTO> customers) {
        JsonNode addressData = seedDataLoader.loadTree("seed/customer-addresses.json");

        for (JsonNode entry : addressData) {
            int idx = entry.get("customerIndex").asInt();
            if (idx >= customers.size()) continue;
            String custId = customers.get(idx).getId();
            String fullName = customers.get(idx).getFullName();

            int addrIdx = 0;
            for (JsonNode a : entry.get("addresses")) {
                try {
                    addressService.addAddress(custId, AddressDTO.Request.builder()
                            .addressLabel(a.get("label").asText())
                            .addressType(a.get("type").asText())
                            .addressLine1(a.get("line1").asText())
                            .addressLine2(a.has("line2") ? a.get("line2").asText() : null)
                            .city(a.get("city").asText())
                            .state(a.get("state").asText())
                            .postalCode(a.get("zip").asText())
                            .country(a.get("country").asText())
                            .phone(a.get("phone").asText())
                            .company(a.has("company") ? a.get("company").asText() : null)
                            .fullName(fullName)
                            .isDefault(a.path("isDefault").asBoolean(false))
                            .build());
                } catch (Exception e) {
                    log.warn("[Seed] Address skipped for customer {}: {}", custId, e.getMessage());
                }
                addrIdx++;
            }
        }
    }

    private void seedCommunicationLogs(CustomerCommunicationService commService,
                                        List<CustomerResponseDTO> customers) {
        JsonNode logsJson = seedDataLoader.loadTree("seed/communication-logs.json");

        for (JsonNode logEntry : logsJson) {
            try {
                int idx = logEntry.get("customerIndex").asInt();
                if (idx >= customers.size()) continue;
                boolean followUp = logEntry.get("requiresFollowUp").asBoolean();

                commService.addLog(customers.get(idx).getId(),
                        CommunicationLogDTO.Request.builder()
                                .channel(logEntry.get("channel").asText())
                                .direction(logEntry.get("direction").asText())
                                .subject(logEntry.get("subject").asText())
                                .body(logEntry.get("body").asText())
                                .requiresFollowUp(followUp)
                                .followUpAt(followUp ? LocalDateTime.now().plusDays(7) : null)
                                .build());
            } catch (Exception e) {
                log.warn("[Seed] Comm log skipped: {}", e.getMessage());
            }
        }
    }

    private void seedWishlists(CustomerWishlistService wishlistService,
                               List<CustomerResponseDTO> customers,
                               List<ProductSummaryDTO> products) {
        if (products.isEmpty()) return;
        JsonNode wishlistsJson = seedDataLoader.loadTree("seed/customer-wishlists.json");

        for (JsonNode wl : wishlistsJson) {
            int custIdx = wl.get("customerIndex").asInt();
            if (custIdx >= customers.size()) continue;
            String custId = customers.get(custIdx).getId();

            try {
                var created = wishlistService.createWishlist(custId,
                        WishlistDTO.Request.builder()
                                .name(wl.get("name").asText())
                                .visibility(wl.get("visibility").asText())
                                .isDefault(wl.get("isDefault").asBoolean())
                                .build());
                for (JsonNode pidx : wl.get("productIndices")) {
                    int idx = pidx.asInt() % products.size();
                    wishlistService.addItem(custId, created.getId(),
                            WishlistDTO.AddItemRequest.builder()
                                    .productId(products.get(idx).getId())
                                    .note("Added during browsing")
                                    .build());
                }
            } catch (Exception e) {
                log.warn("[Seed] Wishlist skipped for {}: {}", custId, e.getMessage());
            }
        }
    }

    private void seedSavedCarts(CustomerSavedCartService savedCartService,
                                List<CustomerResponseDTO> customers,
                                List<ProductSummaryDTO> products) {
        if (products.isEmpty()) return;
        JsonNode cartsJson = seedDataLoader.loadTree("seed/customer-saved-carts.json");

        for (JsonNode cart : cartsJson) {
            int custIdx = cart.get("customerIndex").asInt();
            if (custIdx >= customers.size()) continue;
            String custId = customers.get(custIdx).getId();

            try {
                List<SavedCartDTO.ItemRequest> items = new ArrayList<>();
                JsonNode indices = cart.get("productIndices");
                JsonNode quantities = cart.get("quantities");

                for (int i = 0; i < indices.size(); i++) {
                    int pidx = indices.get(i).asInt() % products.size();
                    items.add(SavedCartDTO.ItemRequest.builder()
                            .productId(products.get(pidx).getId())
                            .quantity(quantities.get(i).asInt())
                            .build());
                }

                savedCartService.saveCart(custId, SavedCartDTO.Request.builder()
                        .cartName(cart.get("cartName").asText())
                        .couponCode(cart.has("couponCode") ? cart.get("couponCode").asText() : null)
                        .items(items)
                        .build());
            } catch (Exception e) {
                log.warn("[Seed] Saved cart skipped for {}: {}", custId, e.getMessage());
            }
        }
    }
}
