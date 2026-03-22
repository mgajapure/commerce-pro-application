package com.commerce_pro_backend.common.data;

import com.commerce_pro_backend.catalog.attribute.dto.AttributeDto;
import com.commerce_pro_backend.catalog.attribute.service.AttributeService;
import com.commerce_pro_backend.catalog.brand.dto.BrandDto;
import com.commerce_pro_backend.catalog.brand.service.BrandService;
import com.commerce_pro_backend.catalog.category.entity.Category;
import com.commerce_pro_backend.catalog.collection.dto.CollectionDto;
import com.commerce_pro_backend.catalog.collection.service.CollectionService;
import com.commerce_pro_backend.catalog.product.dto.ProductAttributeDTO;
import com.commerce_pro_backend.catalog.product.dto.ProductRequestDTO;
import com.commerce_pro_backend.catalog.product.dto.ProductSummaryDTO;
import com.commerce_pro_backend.catalog.product.dto.ProductVariantDTO;
import com.commerce_pro_backend.catalog.product.service.ProductService;
import com.commerce_pro_backend.catalog.review.dto.ReviewDto;
import com.commerce_pro_backend.catalog.review.service.ReviewService;
import com.commerce_pro_backend.catalog.seo.dto.SeoDto;
import com.commerce_pro_backend.catalog.seo.service.SeoService;
import com.commerce_pro_backend.customer.dto.*;
import com.commerce_pro_backend.customer.service.*;
import com.commerce_pro_backend.inventory.dto.InventoryFilterDTO;
import com.commerce_pro_backend.inventory.dto.InventoryRequestDTO;
import com.commerce_pro_backend.inventory.dto.StockTransferRequestDTO;
import com.commerce_pro_backend.inventory.dto.StockUpdateRequestDTO;
import com.commerce_pro_backend.inventory.dto.WarehouseDTO;
import com.commerce_pro_backend.inventory.dto.WarehouseRequestDTO;
import com.commerce_pro_backend.inventory.forecast.dto.DemandForecastDto;
import com.commerce_pro_backend.inventory.forecast.service.DemandForecastService;
import com.commerce_pro_backend.inventory.service.InventoryService;
import com.commerce_pro_backend.inventory.valuation.dto.InventoryValuationDto;
import com.commerce_pro_backend.inventory.valuation.service.InventoryValuationService;
import com.commerce_pro_backend.notification.dto.CreateNotificationRequest;
import com.commerce_pro_backend.notification.enums.NotificationChannel;
import com.commerce_pro_backend.notification.enums.NotificationPriority;
import com.commerce_pro_backend.notification.enums.NotificationType;
import com.commerce_pro_backend.notification.service.NotificationService;
import com.commerce_pro_backend.order.dto.CreateOrderRequestDTO;
import com.commerce_pro_backend.order.dto.OrderAddressDTO;
import com.commerce_pro_backend.order.dto.OrderCancelRequest;
import com.commerce_pro_backend.order.dto.OrderFilterDTO;
import com.commerce_pro_backend.order.dto.OrderHoldRequest;
import com.commerce_pro_backend.order.dto.OrderItemRequestDTO;
import com.commerce_pro_backend.order.dto.OrderResponseDTO;
import com.commerce_pro_backend.order.dto.OrderSummaryDTO;
import com.commerce_pro_backend.order.dto.TrackingUpdateRequest;
import com.commerce_pro_backend.order.enums.OrderSource;
import com.commerce_pro_backend.order.service.OrderService;
import com.commerce_pro_backend.user_identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Development-only data initialiser.
 *
 * Execution order (controlled via Spring @Order):
 *   1. Products   — catalogue must exist before anything else can reference it
 *   2. Inventory  — warehouses + stock records per product
 *   3. Customers  — customer records + groups (needed by orders & reviews)
 *   4. Catalog    — brands, categories, attributes, collections, reviews, SEO
 *   5. Orders     — real order lifecycle scenarios using seeded products & customers
 *   6. Fulfillment — carriers, pick lists, shipments
 *   7. Inventory Extended — stock adjustments, transfers, forecasts, valuations
 *   8. Notifications — seed notifications referencing actual DB data
 *
 * DDL is create-drop so the schema is always fresh on restart.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    // -------------------------------------------------------------------------
    // 1. PRODUCTS
    // -------------------------------------------------------------------------

    @Bean
    @Profile("dev")
    @Order(1)
    CommandLineRunner seedProducts(ProductService productService) {
        return args -> {
            log.info("━━━ [Seed] Products ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            try {
                productService.getAllProducts().forEach(p -> productService.deleteProduct(p.getId()));
                log.info("[Seed] Cleared existing products");
            } catch (Exception e) {
                log.warn("[Seed] Could not clear products: {}", e.getMessage());
            }

            List<ProductRequestDTO> catalogue = buildProductCatalogue();
            int created = 0;
            for (ProductRequestDTO req : catalogue) {
                try {
                    productService.createProduct(req);
                    created++;
                } catch (Exception e) {
                    log.error("[Seed] Failed to create product '{}': {}", req.getName(), e.getMessage());
                }
            }
            log.info("[Seed] Products complete — {} / {} created", created, catalogue.size());
        };
    }

    // -------------------------------------------------------------------------
    // 2. INVENTORY (warehouses + stock)
    // -------------------------------------------------------------------------

    @Bean
    @Profile("dev")
    @Order(2)
    CommandLineRunner seedInventory(ProductService productService,
                                    InventoryService inventoryService) {
        return args -> {
            log.info("━━━ [Seed] Inventory ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            List<ProductSummaryDTO> products = productService.getAllProducts();
            if (products.isEmpty()) {
                log.warn("[Seed] No products found — skipping inventory seed");
                return;
            }

            WarehouseDTO mainWarehouse = createMainWarehouse(inventoryService);
            WarehouseDTO westWarehouse = createWestWarehouse(inventoryService);

            int count = 0;
            String[] zones  = {"A", "B", "C", "D"};
            String[] aisles = {"01", "02", "03", "04", "05"};

            for (int i = 0; i < products.size(); i++) {
                ProductSummaryDTO product = products.get(i);
                int baseStock             = product.getStock();
                BigDecimal unitCost       = unitCost(product.getPrice());

                try {
                    int mainQty      = proportional(baseStock, 0.70, 1.00);
                    int mainReserved = proportional(mainQty,   0.05, 0.15);
                    inventoryService.createInventory(
                            buildInventoryRequest(
                                    product.getId(), mainWarehouse.getId(),
                                    mainQty, mainReserved, baseStock, unitCost,
                                    zones[i % 4], aisles[i % 5], i));

                    if (i % 3 == 0) {
                        int westQty = proportional(baseStock, 0.20, 0.30);
                        inventoryService.createInventory(
                                buildInventoryRequest(
                                        product.getId(), westWarehouse.getId(),
                                        westQty, 0, baseStock, unitCost,
                                        zones[(i + 2) % 4], aisles[(i + 2) % 5], i));
                    }
                    count++;
                } catch (Exception e) {
                    log.error("[Seed] Inventory failed for '{}': {}", product.getName(), e.getMessage());
                }
            }

            try {
                var stats = inventoryService.getInventoryStats();
                log.info("[Seed] Inventory stats — items: {}, value: ${}, in-stock: {}, low: {}, out: {}",
                        stats.getTotalItems(), stats.getTotalInventoryValue(),
                        stats.getInStockCount(), stats.getLowStockCount(), stats.getOutOfStockCount());
            } catch (Exception e) {
                log.warn("[Seed] Could not retrieve inventory stats: {}", e.getMessage());
            }

            log.info("[Seed] Inventory complete — {} products stocked", count);
        };
    }

    // -------------------------------------------------------------------------
    // 3. CUSTOMERS (moved before orders & reviews so they can reference DB)
    // -------------------------------------------------------------------------

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
                authenticateAsSuperAdmin(userDetailsService);

                // ── Groups ─────────────────────────────────────────────────────
                GroupDTO.Response vipGroup = groupService.createGroup(GroupDTO.Request.builder()
                        .name("VIP Buyers").description("High-value repeat customers")
                        .colorHex("#F59E0B").isActive(true).discountPercentage(new BigDecimal("10")).build());

                GroupDTO.Response wholesaleGroup = groupService.createGroup(GroupDTO.Request.builder()
                        .name("Wholesale Accounts").description("Trade and bulk purchasers")
                        .colorHex("#0D9488").isActive(true).discountPercentage(new BigDecimal("15")).build());

                GroupDTO.Response newCustGroup = groupService.createGroup(GroupDTO.Request.builder()
                        .name("New Customers").description("Recently registered customers")
                        .colorHex("#6366F1").isActive(true).build());

                GroupDTO.Response loyaltyGroup = groupService.createGroup(GroupDTO.Request.builder()
                        .name("Loyalty Members").description("Members of the loyalty rewards program")
                        .colorHex("#EC4899").isActive(true).discountPercentage(new BigDecimal("5")).build());

                log.info("[Seed] Created 4 customer groups");

                // ── Create enriched customers ──────────────────────────────────
                List<CustomerResponseDTO> dbCustomers = new ArrayList<>();

                // Customer 0: Alice — VIP, PLATINUM tier candidate
                dbCustomers.add(customerService.createCustomer(CustomerRequestDTO.builder()
                        .firstName("Alice").lastName("Johnson").email("alice.johnson@example.com")
                        .phone("+1-212-555-0101").secondaryPhone("+1-212-555-0201")
                        .dateOfBirth(LocalDate.of(1985, 3, 15)).gender("Female")
                        .companyName("Johnson Enterprises").taxId("EIN-12-3456789")
                        .acquisitionSource("STOREFRONT").marketingOptIn(true).smsOptIn(true)
                        .preferredCurrency("USD").preferredLanguage("en")
                        .groupId(vipGroup.getId())
                        .internalNotes("Top-tier customer since 2022. Always pays on time.")
                        .build()));

                // Customer 1: Bob — Wholesale, GOLD tier candidate
                dbCustomers.add(customerService.createCustomer(CustomerRequestDTO.builder()
                        .firstName("Bob").lastName("Smith").email("bob.smith@example.com")
                        .phone("+1-310-555-0102").dateOfBirth(LocalDate.of(1990, 7, 22)).gender("Male")
                        .companyName("Smith Trading Co.").taxId("EIN-98-7654321")
                        .acquisitionSource("STOREFRONT").marketingOptIn(true).smsOptIn(false)
                        .preferredCurrency("USD").preferredLanguage("en")
                        .groupId(wholesaleGroup.getId())
                        .internalNotes("Bulk buyer. Prefers invoice-based payment.")
                        .build()));

                // Customer 2: Carol — Loyalty member, SILVER
                dbCustomers.add(customerService.createCustomer(CustomerRequestDTO.builder()
                        .firstName("Carol").lastName("Williams").email("carol.williams@example.com")
                        .phone("+1-312-555-0103").dateOfBirth(LocalDate.of(1992, 11, 8)).gender("Female")
                        .acquisitionSource("REFERRAL").marketingOptIn(true).smsOptIn(true)
                        .preferredCurrency("USD").preferredLanguage("en")
                        .groupId(loyaltyGroup.getId()).build()));

                // Customer 3: David — New customer
                dbCustomers.add(customerService.createCustomer(CustomerRequestDTO.builder()
                        .firstName("David").lastName("Brown").email("david.brown@example.com")
                        .phone("+1-415-555-0104").dateOfBirth(LocalDate.of(1988, 1, 30)).gender("Male")
                        .acquisitionSource("STOREFRONT").marketingOptIn(false)
                        .preferredCurrency("USD").preferredLanguage("en")
                        .groupId(newCustGroup.getId()).build()));

                // Customer 4: Eve — Will be SUSPENDED
                dbCustomers.add(customerService.createCustomer(CustomerRequestDTO.builder()
                        .firstName("Eve").lastName("Davis").email("eve.davis@example.com")
                        .phone("+1-206-555-0105").dateOfBirth(LocalDate.of(1995, 6, 18)).gender("Female")
                        .acquisitionSource("SOCIAL").marketingOptIn(true)
                        .preferredCurrency("USD").preferredLanguage("en")
                        .internalNotes("Account under review for suspicious activity.")
                        .build()));

                // Customer 5: Frank — VIP
                dbCustomers.add(customerService.createCustomer(CustomerRequestDTO.builder()
                        .firstName("Frank").lastName("Miller").email("frank.miller@example.com")
                        .phone("+1-713-555-0106").secondaryPhone("+1-713-555-0206")
                        .dateOfBirth(LocalDate.of(1978, 9, 5)).gender("Male")
                        .companyName("Miller & Associates")
                        .acquisitionSource("STOREFRONT").marketingOptIn(true).smsOptIn(true)
                        .preferredCurrency("USD").preferredLanguage("en")
                        .groupId(vipGroup.getId())
                        .internalNotes("Prefers email communication. Ships to multiple offices.")
                        .build()));

                // Customer 6: Grace — Loyalty member
                dbCustomers.add(customerService.createCustomer(CustomerRequestDTO.builder()
                        .firstName("Grace").lastName("Lee").email("grace.lee@example.com")
                        .phone("+1-602-555-0107").dateOfBirth(LocalDate.of(1993, 4, 12)).gender("Female")
                        .acquisitionSource("EMAIL").marketingOptIn(true).smsOptIn(true)
                        .preferredCurrency("USD").preferredLanguage("en")
                        .groupId(loyaltyGroup.getId()).build()));

                // Customer 7: Henry — will be INACTIVE
                dbCustomers.add(customerService.createCustomer(CustomerRequestDTO.builder()
                        .firstName("Henry").lastName("Wilson").email("henry.wilson@example.com")
                        .phone("+1-305-555-0108").dateOfBirth(LocalDate.of(1982, 12, 25)).gender("Male")
                        .acquisitionSource("STOREFRONT").marketingOptIn(false)
                        .preferredCurrency("USD").preferredLanguage("en").build()));

                // Customer 8: Irene — Wholesale
                dbCustomers.add(customerService.createCustomer(CustomerRequestDTO.builder()
                        .firstName("Irene").lastName("Taylor").email("irene.taylor@example.com")
                        .phone("+1-617-555-0109").dateOfBirth(LocalDate.of(1987, 8, 2)).gender("Female")
                        .companyName("Taylor Retail Group").taxId("EIN-55-1234567")
                        .acquisitionSource("REFERRAL").marketingOptIn(true)
                        .preferredCurrency("USD").preferredLanguage("en")
                        .groupId(wholesaleGroup.getId()).build()));

                // Customer 9: James — New customer
                dbCustomers.add(customerService.createCustomer(CustomerRequestDTO.builder()
                        .firstName("James").lastName("Anderson").email("james.anderson@example.com")
                        .phone("+1-646-555-0110").dateOfBirth(LocalDate.of(1996, 2, 14)).gender("Male")
                        .acquisitionSource("STOREFRONT").marketingOptIn(true)
                        .preferredCurrency("USD").preferredLanguage("en")
                        .groupId(newCustGroup.getId()).build()));

                // Customer 10: Karen — will be BLACKLISTED
                dbCustomers.add(customerService.createCustomer(CustomerRequestDTO.builder()
                        .firstName("Karen").lastName("Martinez").email("karen.martinez@example.com")
                        .phone("+1-214-555-0111").dateOfBirth(LocalDate.of(1991, 10, 7)).gender("Female")
                        .acquisitionSource("SOCIAL").marketingOptIn(false)
                        .preferredCurrency("USD").preferredLanguage("en").build()));

                // Customer 11: Larry — Loyalty member
                dbCustomers.add(customerService.createCustomer(CustomerRequestDTO.builder()
                        .firstName("Larry").lastName("Garcia").email("larry.garcia@example.com")
                        .phone("+1-408-555-0112").dateOfBirth(LocalDate.of(1984, 5, 20)).gender("Male")
                        .acquisitionSource("STOREFRONT").marketingOptIn(true).smsOptIn(true)
                        .preferredCurrency("USD").preferredLanguage("en")
                        .groupId(loyaltyGroup.getId()).build()));

                // Customer 12: Megan — will be fraud-flagged (resolved)
                dbCustomers.add(customerService.createCustomer(CustomerRequestDTO.builder()
                        .firstName("Megan").lastName("Thomas").email("megan.thomas@example.com")
                        .phone("+1-503-555-0113").dateOfBirth(LocalDate.of(1994, 7, 11)).gender("Female")
                        .acquisitionSource("EMAIL").marketingOptIn(true)
                        .preferredCurrency("USD").preferredLanguage("en").build()));

                // Customer 13: Nathan — New customer
                dbCustomers.add(customerService.createCustomer(CustomerRequestDTO.builder()
                        .firstName("Nathan").lastName("Jackson").email("nathan.jackson@example.com")
                        .phone("+1-702-555-0114").dateOfBirth(LocalDate.of(1999, 3, 28)).gender("Male")
                        .acquisitionSource("STOREFRONT").marketingOptIn(false)
                        .preferredCurrency("USD").preferredLanguage("en")
                        .groupId(newCustGroup.getId()).build()));

                // Customer 14: Olivia — VIP
                dbCustomers.add(customerService.createCustomer(CustomerRequestDTO.builder()
                        .firstName("Olivia").lastName("White").email("olivia.white@example.com")
                        .phone("+1-404-555-0115").secondaryPhone("+1-404-555-0215")
                        .dateOfBirth(LocalDate.of(1986, 9, 16)).gender("Female")
                        .companyName("White Digital Solutions")
                        .acquisitionSource("REFERRAL").marketingOptIn(true).smsOptIn(true)
                        .preferredCurrency("USD").preferredLanguage("en")
                        .groupId(vipGroup.getId())
                        .internalNotes("Referred by Alice Johnson. High engagement.")
                        .build()));

                log.info("[Seed] Created {} customers with enriched profiles", dbCustomers.size());

                // ── Tags ───────────────────────────────────────────────────────
                customerService.updateTags(dbCustomers.get(0).getId(), TagsUpdateDTO.builder().tags(Set.of("vip", "enterprise", "early-adopter")).build());
                customerService.updateTags(dbCustomers.get(1).getId(), TagsUpdateDTO.builder().tags(Set.of("wholesale", "bulk-buyer", "net-30")).build());
                customerService.updateTags(dbCustomers.get(2).getId(), TagsUpdateDTO.builder().tags(Set.of("loyalty", "referral", "repeat-buyer")).build());
                customerService.updateTags(dbCustomers.get(5).getId(), TagsUpdateDTO.builder().tags(Set.of("vip", "priority-support")).build());
                customerService.updateTags(dbCustomers.get(8).getId(), TagsUpdateDTO.builder().tags(Set.of("wholesale", "trade-account")).build());
                customerService.updateTags(dbCustomers.get(11).getId(), TagsUpdateDTO.builder().tags(Set.of("loyalty", "points-active")).build());
                customerService.updateTags(dbCustomers.get(14).getId(), TagsUpdateDTO.builder().tags(Set.of("vip", "digital", "referral")).build());
                log.info("[Seed] Tags assigned to 7 customers");

                // ── Loyalty points ─────────────────────────────────────────────
                customerService.adjustLoyaltyPoints(dbCustomers.get(0).getId(), LoyaltyAdjustDTO.builder().points(2500).description("Welcome bonus + purchase rewards").build());
                customerService.adjustLoyaltyPoints(dbCustomers.get(1).getId(), LoyaltyAdjustDTO.builder().points(1800).description("Bulk purchase rewards").build());
                customerService.adjustLoyaltyPoints(dbCustomers.get(2).getId(), LoyaltyAdjustDTO.builder().points(950).description("Referral bonus + purchases").build());
                customerService.adjustLoyaltyPoints(dbCustomers.get(5).getId(), LoyaltyAdjustDTO.builder().points(3200).description("VIP tier rewards accumulation").build());
                customerService.adjustLoyaltyPoints(dbCustomers.get(6).getId(), LoyaltyAdjustDTO.builder().points(600).description("Email campaign bonus").build());
                customerService.adjustLoyaltyPoints(dbCustomers.get(11).getId(), LoyaltyAdjustDTO.builder().points(1100).description("Loyalty program rewards").build());
                customerService.adjustLoyaltyPoints(dbCustomers.get(14).getId(), LoyaltyAdjustDTO.builder().points(2000).description("VIP referral rewards").build());
                log.info("[Seed] Loyalty points assigned to 7 customers");

                // ── Status variety ─────────────────────────────────────────────
                customerService.setStatus(dbCustomers.get(4).getId(), "SUSPENDED");   // Eve
                customerService.setStatus(dbCustomers.get(7).getId(), "INACTIVE");    // Henry

                // Blacklist Karen
                customerService.blacklist(dbCustomers.get(10).getId(),
                        BlacklistDTO.BlacklistRequest.builder().reason("Multiple chargeback disputes and policy violations").build());

                // Flag Megan for fraud then resolve it
                customerService.flagFraud(dbCustomers.get(12).getId(),
                        BlacklistDTO.FraudFlagRequest.builder()
                                .reason("Unusual purchasing pattern detected")
                                .evidence("3 high-value orders from different IPs within 10 minutes").build());
                customerService.resolveFraud(dbCustomers.get(12).getId(),
                        BlacklistDTO.FraudResolveRequest.builder()
                                .resolution("Verified with customer — legitimate orders from office and mobile").build());

                log.info("[Seed] Status variety: 1 suspended, 1 inactive, 1 blacklisted, 1 fraud-flagged (resolved)");

                // ── Addresses (2-3 per customer) ───────────────────────────────
                seedCustomerAddresses(addressService, dbCustomers);
                log.info("[Seed] Addresses seeded for all customers");

                // ── Communication logs ─────────────────────────────────────────
                seedCommunicationLogs(commService, dbCustomers);
                log.info("[Seed] Communication logs seeded");

                // ── Wishlists ──────────────────────────────────────────────────
                List<ProductSummaryDTO> products = productService.getAllProducts();
                seedWishlists(wishlistService, dbCustomers, products);
                log.info("[Seed] Wishlists seeded");

                // ── Saved carts ────────────────────────────────────────────────
                seedSavedCarts(savedCartService, dbCustomers, products);
                log.info("[Seed] Saved carts seeded");

                log.info("[Seed] Customers complete — {} created with full data", dbCustomers.size());

            } catch (Exception e) {
                log.error("[Seed] Customer seed failed: {}", e.getMessage(), e);
            } finally {
                clearSecurityContext();
            }
        };
    }

    // ── Customer sub-data seed helpers ──────────────────────────────────────

    private void seedCustomerAddresses(CustomerAddressService addressService, List<CustomerResponseDTO> customers) {
        // Address pool: {label, type, line1, line2, city, state, zip, country, phone, company}
        String[][][] addressPool = {
            { // Customer 0: Alice — 3 addresses
                {"Home", "SHIPPING", "742 Evergreen Terrace", "Apt 4B", "New York", "NY", "10001", "USA", "+1-212-555-0101", null},
                {"Office", "BILLING", "350 Fifth Avenue", "Floor 22", "New York", "NY", "10118", "USA", "+1-212-555-0201", "Johnson Enterprises"},
                {"Warehouse", "BOTH", "100 Industrial Blvd", null, "Newark", "NJ", "07102", "USA", "+1-973-555-0301", "Johnson Enterprises"},
            },
            { // Customer 1: Bob — 2 addresses
                {"Home", "BOTH", "1600 Vine Street", null, "Los Angeles", "CA", "90028", "USA", "+1-310-555-0102", null},
                {"Business", "BILLING", "9000 Sunset Blvd", "Suite 500", "West Hollywood", "CA", "90069", "USA", "+1-310-555-0202", "Smith Trading Co."},
            },
            { // Customer 2: Carol — 2 addresses
                {"Home", "BOTH", "233 S Wacker Dr", "Unit 12A", "Chicago", "IL", "60606", "USA", "+1-312-555-0103", null},
                {"Parents", "SHIPPING", "456 Oak Avenue", null, "Naperville", "IL", "60540", "USA", "+1-630-555-0103", null},
            },
            { // Customer 3: David — 2 addresses
                {"Home", "SHIPPING", "1 Market Street", "Apt 8C", "San Francisco", "CA", "94105", "USA", "+1-415-555-0104", null},
                {"Office", "BILLING", "500 Terry Francois Blvd", null, "San Francisco", "CA", "94158", "USA", "+1-415-555-0204", null},
            },
            { // Customer 4: Eve — 1 address
                {"Home", "BOTH", "400 Broad Street", null, "Seattle", "WA", "98109", "USA", "+1-206-555-0105", null},
            },
            { // Customer 5: Frank — 3 addresses
                {"Home", "SHIPPING", "1000 Main Street", null, "Houston", "TX", "77002", "USA", "+1-713-555-0106", null},
                {"Downtown Office", "BILLING", "800 Bell Street", "Suite 1200", "Houston", "TX", "77019", "USA", "+1-713-555-0206", "Miller & Associates"},
                {"Branch Office", "SHIPPING", "200 Congress Ave", "Floor 10", "Austin", "TX", "78701", "USA", "+1-512-555-0106", "Miller & Associates"},
            },
            { // Customer 6: Grace — 2 addresses
                {"Home", "BOTH", "3300 E Camelback Rd", "Apt 205", "Phoenix", "AZ", "85018", "USA", "+1-602-555-0107", null},
                {"Gift Address", "SHIPPING", "7000 N 16th St", null, "Phoenix", "AZ", "85020", "USA", "+1-602-555-0207", null},
            },
            { // Customer 7: Henry — 1 address
                {"Home", "BOTH", "1200 Brickell Ave", "PH1", "Miami", "FL", "33131", "USA", "+1-305-555-0108", null},
            },
            { // Customer 8: Irene — 2 addresses
                {"Home", "SHIPPING", "200 Clarendon St", null, "Boston", "MA", "02116", "USA", "+1-617-555-0109", null},
                {"Retail HQ", "BILLING", "100 Federal Street", "Floor 30", "Boston", "MA", "02110", "USA", "+1-617-555-0209", "Taylor Retail Group"},
            },
            { // Customer 9: James — 2 addresses
                {"Home", "BOTH", "60 Hudson Street", "Apt 3D", "New York", "NY", "10013", "USA", "+1-646-555-0110", null},
                {"College", "SHIPPING", "120 Broadway", null, "New York", "NY", "10271", "USA", "+1-646-555-0210", null},
            },
            { // Customer 10: Karen — 1 address
                {"Home", "BOTH", "2000 Ross Avenue", null, "Dallas", "TX", "75201", "USA", "+1-214-555-0111", null},
            },
            { // Customer 11: Larry — 2 addresses
                {"Home", "BOTH", "1 Infinite Loop", null, "San Jose", "CA", "95112", "USA", "+1-408-555-0112", null},
                {"Vacation", "SHIPPING", "500 Beach Dr", null, "Santa Cruz", "CA", "95060", "USA", "+1-831-555-0112", null},
            },
            { // Customer 12: Megan — 2 addresses
                {"Home", "BOTH", "800 SW Broadway", "Apt 610", "Portland", "OR", "97205", "USA", "+1-503-555-0113", null},
                {"Work", "SHIPPING", "1120 NW Couch St", "Suite 200", "Portland", "OR", "97209", "USA", "+1-503-555-0213", null},
            },
            { // Customer 13: Nathan — 1 address
                {"Home", "BOTH", "3600 Las Vegas Blvd S", "Unit 2510", "Las Vegas", "NV", "89109", "USA", "+1-702-555-0114", null},
            },
            { // Customer 14: Olivia — 3 addresses
                {"Home", "SHIPPING", "191 Peachtree St NE", "Apt 1502", "Atlanta", "GA", "30303", "USA", "+1-404-555-0115", null},
                {"Office", "BILLING", "3344 Peachtree Rd NE", "Suite 800", "Atlanta", "GA", "30326", "USA", "+1-404-555-0215", "White Digital Solutions"},
                {"Parents", "SHIPPING", "500 Magnolia Lane", null, "Savannah", "GA", "31401", "USA", "+1-912-555-0115", null},
            },
        };

        for (int i = 0; i < customers.size() && i < addressPool.length; i++) {
            String custId = customers.get(i).getId();
            for (int j = 0; j < addressPool[i].length; j++) {
                String[] a = addressPool[i][j];
                try {
                    addressService.addAddress(custId, AddressDTO.Request.builder()
                            .addressLabel(a[0]).addressType(a[1]).addressLine1(a[2]).addressLine2(a[3])
                            .city(a[4]).state(a[5]).postalCode(a[6]).country(a[7])
                            .phone(a[8]).company(a[9])
                            .fullName(customers.get(i).getFullName())
                            .isDefault(j == 0) // first address is default
                            .build());
                } catch (Exception e) {
                    log.warn("[Seed] Address skipped for customer {}: {}", custId, e.getMessage());
                }
            }
        }
    }

    private void seedCommunicationLogs(CustomerCommunicationService commService, List<CustomerResponseDTO> customers) {
        // {customerIndex, channel, direction, subject, body, requiresFollowUp}
        Object[][] logs = {
            {0, "EMAIL", "OUTBOUND", "Welcome to Commerce Pro!", "Dear Alice, thank you for joining us. Your VIP account has been activated.", false},
            {0, "PHONE", "INBOUND", "Order inquiry", "Alice called about order delivery timeline for bulk purchase. Confirmed 3-5 business days.", false},
            {0, "EMAIL", "OUTBOUND", "VIP Exclusive Offer", "Exclusive 20% discount on new arrivals for VIP members.", false},
            {1, "EMAIL", "OUTBOUND", "Wholesale Account Setup", "Dear Bob, your wholesale account has been approved with net-30 terms.", false},
            {1, "PHONE", "INBOUND", "Bulk pricing request", "Bob inquired about volume discounts for 500+ units. Forwarded to sales team.", true},
            {2, "EMAIL", "OUTBOUND", "Referral Bonus Applied", "Carol, your referral bonus of 200 loyalty points has been credited.", false},
            {2, "CHAT", "INBOUND", "Product availability", "Carol asked about restocking timeline for SKU-1001. Expected next week.", false},
            {3, "EMAIL", "OUTBOUND", "Welcome Email", "Welcome David! Explore our catalog and enjoy free shipping on your first order.", false},
            {4, "EMAIL", "OUTBOUND", "Account Suspension Notice", "Dear Eve, your account has been temporarily suspended pending review.", false},
            {4, "PHONE", "INBOUND", "Appeal request", "Eve called to appeal account suspension. Scheduled review for next week.", true},
            {5, "EMAIL", "OUTBOUND", "Priority Support Confirmation", "Frank, your priority support tier is now active.", false},
            {5, "PHONE", "INBOUND", "Multi-office shipping setup", "Frank requested shipping to 3 different office locations. Updated address book.", false},
            {5, "SMS", "OUTBOUND", "Delivery confirmation", "Your order has been delivered to Downtown Office. Thank you!", false},
            {6, "EMAIL", "OUTBOUND", "Loyalty Points Statement", "Grace, you have 600 points. Redeem them on your next purchase.", false},
            {6, "CHAT", "INBOUND", "Return request", "Grace requested a return for a defective item. RMA created.", false},
            {7, "EMAIL", "OUTBOUND", "We Miss You!", "Henry, it's been a while since your last visit. Here's 15% off to welcome you back.", false},
            {8, "EMAIL", "OUTBOUND", "Wholesale Catalog Update", "Irene, new products have been added to the wholesale catalog.", false},
            {8, "PHONE", "INBOUND", "Payment terms discussion", "Irene discussed extending payment terms to net-45. Escalated to finance.", true},
            {9, "EMAIL", "OUTBOUND", "Welcome Email", "Welcome James! Check out our trending products.", false},
            {10, "EMAIL", "OUTBOUND", "Account Warning", "Karen, we've noticed unusual activity on your account.", false},
            {10, "PHONE", "INBOUND", "Dispute call", "Karen called regarding chargeback dispute. Refused to provide documentation.", false},
            {11, "EMAIL", "OUTBOUND", "Points Earned!", "Larry, you earned 150 loyalty points from your recent purchase!", false},
            {11, "CHAT", "INBOUND", "Shipping question", "Larry asked about international shipping options. Provided rate card.", false},
            {12, "EMAIL", "OUTBOUND", "Fraud Review Complete", "Megan, your account review is complete. No further action needed.", false},
            {12, "PHONE", "INBOUND", "Verification call", "Megan called to verify her recent orders. Confirmed identity successfully.", false},
            {14, "EMAIL", "OUTBOUND", "VIP Welcome Package", "Olivia, your VIP welcome package is on its way!", false},
            {14, "IN_PERSON", "INBOUND", "Store visit - product consultation", "Olivia visited the showroom for a product demo. Very interested in premium line.", false},
        };

        for (Object[] log : logs) {
            try {
                int idx = (int) log[0];
                commService.addLog(customers.get(idx).getId(), CommunicationLogDTO.Request.builder()
                        .channel((String) log[1]).direction((String) log[2])
                        .subject((String) log[3]).body((String) log[4])
                        .requiresFollowUp((Boolean) log[5])
                        .followUpAt((Boolean) log[5] ? LocalDateTime.now().plusDays(7) : null)
                        .build());
            } catch (Exception e) {
                DataInitializer.log.warn("[Seed] Comm log skipped: {}", e.getMessage());
            }
        }
    }

    private void seedWishlists(CustomerWishlistService wishlistService,
                               List<CustomerResponseDTO> customers,
                               List<ProductSummaryDTO> products) {
        if (products.isEmpty()) return;

        // Customer 0: Alice — 2 wishlists
        seedWishlist(wishlistService, customers.get(0).getId(), "My Favorites", "PRIVATE", true, products, new int[]{0, 2, 5});
        seedWishlist(wishlistService, customers.get(0).getId(), "Gift Ideas", "SHARED", false, products, new int[]{1, 3, 7});

        // Customer 1: Bob — 1 wishlist (bulk items)
        seedWishlist(wishlistService, customers.get(1).getId(), "Wholesale Picks", "PRIVATE", true, products, new int[]{0, 1, 4, 6, 8});

        // Customer 2: Carol — 1 wishlist
        seedWishlist(wishlistService, customers.get(2).getId(), "My Wishlist", "PRIVATE", true, products, new int[]{2, 5, 9});

        // Customer 5: Frank — 2 wishlists
        seedWishlist(wishlistService, customers.get(5).getId(), "Office Supplies", "PRIVATE", true, products, new int[]{0, 3, 6});
        seedWishlist(wishlistService, customers.get(5).getId(), "Personal", "PUBLIC", false, products, new int[]{1, 4});

        // Customer 6: Grace — 1 wishlist
        seedWishlist(wishlistService, customers.get(6).getId(), "Wishlist", "PRIVATE", true, products, new int[]{2, 7, 10 % products.size()});

        // Customer 9: James — 1 wishlist
        seedWishlist(wishlistService, customers.get(9).getId(), "Must Haves", "PRIVATE", true, products, new int[]{0, 5});

        // Customer 11: Larry — 1 wishlist
        seedWishlist(wishlistService, customers.get(11).getId(), "My List", "PUBLIC", true, products, new int[]{3, 6, 8});

        // Customer 14: Olivia — 2 wishlists
        seedWishlist(wishlistService, customers.get(14).getId(), "Premium Collection", "PRIVATE", true, products, new int[]{0, 1, 2, 3});
        seedWishlist(wishlistService, customers.get(14).getId(), "For the Team", "SHARED", false, products, new int[]{4, 5, 6});
    }

    private void seedWishlist(CustomerWishlistService wishlistService, String custId,
                              String name, String visibility, boolean isDefault,
                              List<ProductSummaryDTO> products, int[] indices) {
        try {
            var wl = wishlistService.createWishlist(custId, WishlistDTO.Request.builder()
                    .name(name).visibility(visibility).isDefault(isDefault).build());
            for (int idx : indices) {
                if (idx < products.size()) {
                    wishlistService.addItem(custId, wl.getId(), WishlistDTO.AddItemRequest.builder()
                            .productId(products.get(idx).getId())
                            .note("Added during browsing")
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("[Seed] Wishlist skipped for {}: {}", custId, e.getMessage());
        }
    }

    private void seedSavedCarts(CustomerSavedCartService savedCartService,
                                List<CustomerResponseDTO> customers,
                                List<ProductSummaryDTO> products) {
        if (products.isEmpty()) return;

        // Customer 0: Alice — active cart
        seedSavedCart(savedCartService, customers.get(0).getId(), "Weekly Order", null, products, new int[]{0, 2}, new int[]{2, 1});

        // Customer 1: Bob — bulk order cart
        seedSavedCart(savedCartService, customers.get(1).getId(), "Bulk Restock", "BULK10", products, new int[]{0, 1, 4, 6}, new int[]{10, 5, 8, 12});

        // Customer 2: Carol — abandoned cart
        seedSavedCart(savedCartService, customers.get(2).getId(), "Saved Cart", null, products, new int[]{3, 5}, new int[]{1, 1});

        // Customer 3: David — abandoned cart (new customer who left)
        seedSavedCart(savedCartService, customers.get(3).getId(), "My Cart", null, products, new int[]{0}, new int[]{1});

        // Customer 5: Frank — 2 carts
        seedSavedCart(savedCartService, customers.get(5).getId(), "Office Order", null, products, new int[]{1, 3, 6}, new int[]{3, 2, 4});
        seedSavedCart(savedCartService, customers.get(5).getId(), "Personal Items", null, products, new int[]{2, 7 % products.size()}, new int[]{1, 1});

        // Customer 6: Grace — cart with coupon
        seedSavedCart(savedCartService, customers.get(6).getId(), "Birthday Shopping", "BDAY15", products, new int[]{0, 5}, new int[]{1, 2});

        // Customer 9: James — abandoned cart
        seedSavedCart(savedCartService, customers.get(9).getId(), "Checkout Later", null, products, new int[]{2, 4}, new int[]{1, 1});

        // Customer 11: Larry — active cart
        seedSavedCart(savedCartService, customers.get(11).getId(), "Loyalty Rewards Cart", "LOYALTY20", products, new int[]{0, 3, 8 % products.size()}, new int[]{1, 2, 1});

        // Customer 14: Olivia — premium cart
        seedSavedCart(savedCartService, customers.get(14).getId(), "Premium Selection", "VIP25", products, new int[]{0, 1, 2}, new int[]{2, 1, 3});
    }

    private void seedSavedCart(CustomerSavedCartService savedCartService, String custId,
                               String cartName, String coupon,
                               List<ProductSummaryDTO> products, int[] indices, int[] quantities) {
        try {
            List<SavedCartDTO.ItemRequest> items = new ArrayList<>();
            for (int i = 0; i < indices.length; i++) {
                if (indices[i] < products.size()) {
                    items.add(SavedCartDTO.ItemRequest.builder()
                            .productId(products.get(indices[i]).getId())
                            .quantity(quantities[i])
                            .build());
                }
            }
            savedCartService.saveCart(custId, SavedCartDTO.Request.builder()
                    .cartName(cartName).couponCode(coupon).items(items).build());
        } catch (Exception e) {
            log.warn("[Seed] Saved cart skipped for {}: {}", custId, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 4. CATALOG (brands, categories, attributes, collections, reviews, SEO)
    // -------------------------------------------------------------------------

    @Bean
    @Profile("dev")
    @Order(4)
    CommandLineRunner seedCatalog(
            BrandService brandService,
            com.commerce_pro_backend.catalog.category.repository.CategoryRepository categoryRepository,
            AttributeService attributeService,
            CollectionService collectionService,
            ReviewService reviewService,
            SeoService seoService,
            ProductService productService,
            com.commerce_pro_backend.customer.repository.CustomerRepository customerRepository,
            UserDetailsService userDetailsService) {
        return args -> {
            log.info("━━━ [Seed] Catalog ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            try {
                authenticateAsSuperAdmin(userDetailsService);

                // -- Brands ------------------------------------------------
                String[][] brandData = {
                    {"Sony", "sony", "Global electronics leader known for audio, gaming, and imaging", null, "https://www.sony.com"},
                    {"TechBrand", "techbrand", "Innovative wearable technology and smart devices", null, null},
                    {"KeyChamp", "keychamp", "Premium mechanical keyboard manufacturer", null, null},
                    {"LogiTech", "logitech", "Computer peripherals and video collaboration", null, "https://www.logitech.com"},
                    {"BoseAudio", "boseaudio", "Premium audio equipment and noise-cancelling technology", null, "https://www.bose.com"},
                    {"Heritage Leather", "heritage-leather", "Handcrafted premium leather goods since 1985", null, null},
                    {"RayStyle", "raystyle", "Designer eyewear with UV protection", null, null},
                    {"LeatherCraft", "leathercraft", "Minimalist leather accessories", null, null},
                    {"StreetWear", "streetwear", "Urban fashion and casual footwear", null, null},
                    {"TimePiece", "timepiece", "Luxury automatic watches and timepieces", null, null},
                    {"BrewMaster", "brewmaster", "Artisanal coffee brewing equipment", null, null},
                    {"HydroLife", "hydrolife", "Eco-friendly hydration solutions", null, null},
                    {"ArtisanHome", "artisanhome", "Handcrafted home and kitchen ceramics", null, null},
                    {"PureAir", "pureair", "Smart air purification systems", null, null},
                    {"FitGear", "fitgear", "Professional fitness and yoga equipment", null, null},
                    {"PowerFit", "powerfit", "Strength training and home gym equipment", null, null},
                    {"NikeRun", "nikerun", "Performance running shoes and athletic gear", null, null},
                    {"FlexFit", "flexfit", "Resistance training and home workout equipment", null, null},
                    {"CanonPro", "canonpro", "Professional photography cameras and lenses", null, null},
                    {"SoundTech", "soundtech", "Wireless audio and noise-cancelling earbuds", null, null},
                    {"SmartLife", "smartlife", "Smart home automation and IoT devices", null, null},
                    {"ComfortSeating", "comfortseating", "Ergonomic office furniture", null, null},
                };

                int brandsCreated = 0;
                for (int i = 0; i < brandData.length; i++) {
                    try {
                        brandService.createBrand(BrandDto.Request.builder()
                                .name(brandData[i][0])
                                .slug(brandData[i][1])
                                .description(brandData[i][2])
                                .logo(brandData[i][3])
                                .website(brandData[i][4])
                                .isActive(true)
                                .isFeatured(i < 6)
                                .sortOrder(i + 1)
                                .build());
                        brandsCreated++;
                    } catch (Exception e) {
                        log.warn("[Seed] Brand '{}' skipped: {}", brandData[i][0], e.getMessage());
                    }
                }
                log.info("[Seed] Created {} brands", brandsCreated);

                // -- Categories (direct repository save) -------------------
                Function<String[], Category> cat = d -> Category.builder()
                        .name(d[0]).slug(d[1]).description(d[2]).imageUrl(d[3])
                        .isActive(true).showInMenu(true).sortOrder(Integer.parseInt(d[4]))
                        .seoTitle(d[5]).seoDescription(d[6])
                        .tenantId("default").createdBy("system").updatedBy("system")
                        .hierarchyLevel(0).build();

                var electronics = categoryRepository.save(cat.apply(new String[]{
                        "Electronics", "electronics", "Electronic devices, gadgets, and accessories",
                        "https://images.unsplash.com/photo-1498049794561-7780e7231661?w=400", "1",
                        "Electronics - Best Deals on Electronic Devices",
                        "Shop the latest electronics including headphones, cameras, smart devices and more."}));
                electronics.setMaterializedPath("/" + electronics.getId());
                categoryRepository.save(electronics);

                var fashion = categoryRepository.save(cat.apply(new String[]{
                        "Fashion", "fashion", "Clothing, shoes, bags, and fashion accessories",
                        "https://images.unsplash.com/photo-1445205170230-053b83016050?w=400", "2",
                        "Fashion - Trending Styles and Accessories",
                        "Discover the latest fashion trends, designer bags, shoes, and accessories."}));
                fashion.setMaterializedPath("/" + fashion.getId());
                categoryRepository.save(fashion);

                var home = categoryRepository.save(cat.apply(new String[]{
                        "Home & Kitchen", "home-kitchen", "Home appliances, kitchenware, and furniture",
                        "https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=400", "3",
                        "Home & Kitchen - Essentials for Your Home",
                        "Browse premium home appliances, kitchenware, and furniture."}));
                home.setMaterializedPath("/" + home.getId());
                categoryRepository.save(home);

                var sports = categoryRepository.save(cat.apply(new String[]{
                        "Sports", "sports", "Fitness equipment, sports gear, and athletic wear",
                        "https://images.unsplash.com/photo-1461896836934-bd45ba8bab8e?w=400", "4",
                        "Sports & Fitness Equipment",
                        "Shop fitness equipment, yoga mats, running shoes, and more."}));
                sports.setMaterializedPath("/" + sports.getId());
                categoryRepository.save(sports);

                // Sub-categories
                String[][] subs = {
                        {"Audio", "audio", "Headphones, speakers, and audio equipment"},
                        {"Wearables", "wearables", "Smartwatches and fitness trackers"},
                        {"Cameras", "cameras", "DSLR and mirrorless cameras"},
                        {"Smart Home", "smart-home", "IoT devices and home automation"},
                };
                for (int i = 0; i < subs.length; i++) {
                    var c = Category.builder().name(subs[i][0]).slug(subs[i][1]).description(subs[i][2])
                            .parent(electronics).hierarchyLevel(1).isActive(true).showInMenu(true)
                            .sortOrder(i + 1).tenantId("default").createdBy("system").updatedBy("system").build();
                    var saved = categoryRepository.save(c);
                    saved.setMaterializedPath(electronics.getMaterializedPath() + "/" + saved.getId());
                    categoryRepository.save(saved);
                }
                String[][] fashionSubs = {
                        {"Bags & Wallets", "bags-wallets", "Backpacks, wallets, and bags"},
                        {"Shoes", "shoes", "Sneakers, running shoes, and footwear"},
                        {"Accessories", "fashion-accessories", "Sunglasses, watches, and more"},
                };
                for (int i = 0; i < fashionSubs.length; i++) {
                    var c = Category.builder().name(fashionSubs[i][0]).slug(fashionSubs[i][1]).description(fashionSubs[i][2])
                            .parent(fashion).hierarchyLevel(1).isActive(true).showInMenu(true)
                            .sortOrder(i + 1).tenantId("default").createdBy("system").updatedBy("system").build();
                    var saved = categoryRepository.save(c);
                    saved.setMaterializedPath(fashion.getMaterializedPath() + "/" + saved.getId());
                    categoryRepository.save(saved);
                }
                String[][] homeSubs = {
                        {"Kitchen", "kitchen", "Kitchenware and cooking equipment"},
                        {"Furniture", "furniture", "Office and home furniture"},
                };
                for (int i = 0; i < homeSubs.length; i++) {
                    var c = Category.builder().name(homeSubs[i][0]).slug(homeSubs[i][1]).description(homeSubs[i][2])
                            .parent(home).hierarchyLevel(1).isActive(true).showInMenu(true)
                            .sortOrder(i + 1).tenantId("default").createdBy("system").updatedBy("system").build();
                    var saved = categoryRepository.save(c);
                    saved.setMaterializedPath(home.getMaterializedPath() + "/" + saved.getId());
                    categoryRepository.save(saved);
                }
                String[][] sportsSubs = {
                        {"Fitness", "fitness", "Yoga mats, resistance bands, and gym gear"},
                        {"Running", "running", "Running shoes and gear"},
                };
                for (int i = 0; i < sportsSubs.length; i++) {
                    var c = Category.builder().name(sportsSubs[i][0]).slug(sportsSubs[i][1]).description(sportsSubs[i][2])
                            .parent(sports).hierarchyLevel(1).isActive(true).showInMenu(true)
                            .sortOrder(i + 1).tenantId("default").createdBy("system").updatedBy("system").build();
                    var saved = categoryRepository.save(c);
                    saved.setMaterializedPath(sports.getMaterializedPath() + "/" + saved.getId());
                    categoryRepository.save(saved);
                }

                log.info("[Seed] Created 4 root categories + 11 sub-categories");

                // -- Attributes --------------------------------------------
                attributeService.createAttribute(AttributeDto.Request.builder()
                        .name("Color").code("color").type("SELECT")
                        .description("Product color options")
                        .isRequired(false).isFilterable(true).isSearchable(true).isActive(true).sortOrder(1)
                        .options(List.of(
                                AttributeDto.OptionRequest.builder().value("Black").label("Black").sortOrder(1).colorHex("#000000").build(),
                                AttributeDto.OptionRequest.builder().value("White").label("White").sortOrder(2).colorHex("#FFFFFF").build(),
                                AttributeDto.OptionRequest.builder().value("Silver").label("Silver").sortOrder(3).colorHex("#C0C0C0").build(),
                                AttributeDto.OptionRequest.builder().value("Blue").label("Blue").sortOrder(4).colorHex("#2563EB").build(),
                                AttributeDto.OptionRequest.builder().value("Red").label("Red").sortOrder(5).colorHex("#DC2626").build(),
                                AttributeDto.OptionRequest.builder().value("Navy").label("Navy").sortOrder(6).colorHex("#1E3A5F").build()))
                        .build());

                attributeService.createAttribute(AttributeDto.Request.builder()
                        .name("Size").code("size").type("SELECT")
                        .description("Product size options")
                        .isRequired(false).isFilterable(true).isSearchable(true).isActive(true).sortOrder(2)
                        .options(List.of(
                                AttributeDto.OptionRequest.builder().value("XS").label("Extra Small").sortOrder(1).build(),
                                AttributeDto.OptionRequest.builder().value("S").label("Small").sortOrder(2).build(),
                                AttributeDto.OptionRequest.builder().value("M").label("Medium").sortOrder(3).build(),
                                AttributeDto.OptionRequest.builder().value("L").label("Large").sortOrder(4).build(),
                                AttributeDto.OptionRequest.builder().value("XL").label("Extra Large").sortOrder(5).build()))
                        .build());

                attributeService.createAttribute(AttributeDto.Request.builder()
                        .name("Material").code("material").type("SELECT")
                        .description("Primary material composition")
                        .isRequired(false).isFilterable(true).isSearchable(false).isActive(true).sortOrder(3)
                        .options(List.of(
                                AttributeDto.OptionRequest.builder().value("Leather").label("Genuine Leather").sortOrder(1).build(),
                                AttributeDto.OptionRequest.builder().value("Canvas").label("Canvas").sortOrder(2).build(),
                                AttributeDto.OptionRequest.builder().value("Metal").label("Metal/Aluminum").sortOrder(3).build(),
                                AttributeDto.OptionRequest.builder().value("Plastic").label("Plastic/Polymer").sortOrder(4).build(),
                                AttributeDto.OptionRequest.builder().value("Ceramic").label("Ceramic").sortOrder(5).build()))
                        .build());

                attributeService.createAttribute(AttributeDto.Request.builder()
                        .name("Connectivity").code("connectivity").type("SELECT")
                        .description("Wireless connectivity type")
                        .isRequired(false).isFilterable(true).isSearchable(false).isActive(true).sortOrder(4)
                        .options(List.of(
                                AttributeDto.OptionRequest.builder().value("Bluetooth 5.0").label("Bluetooth 5.0").sortOrder(1).build(),
                                AttributeDto.OptionRequest.builder().value("WiFi").label("WiFi").sortOrder(2).build(),
                                AttributeDto.OptionRequest.builder().value("USB-C").label("USB-C").sortOrder(3).build(),
                                AttributeDto.OptionRequest.builder().value("NFC").label("NFC").sortOrder(4).build()))
                        .build());

                attributeService.createAttribute(AttributeDto.Request.builder()
                        .name("Weight").code("weight").type("TEXT")
                        .description("Product weight")
                        .isRequired(false).isFilterable(false).isSearchable(false).isActive(true).sortOrder(5)
                        .build());

                attributeService.createAttribute(AttributeDto.Request.builder()
                        .name("Warranty").code("warranty").type("SELECT")
                        .description("Warranty period")
                        .isRequired(false).isFilterable(true).isSearchable(false).isActive(true).sortOrder(6)
                        .options(List.of(
                                AttributeDto.OptionRequest.builder().value("6 Months").label("6 Months").sortOrder(1).build(),
                                AttributeDto.OptionRequest.builder().value("1 Year").label("1 Year").sortOrder(2).build(),
                                AttributeDto.OptionRequest.builder().value("2 Years").label("2 Years").sortOrder(3).build(),
                                AttributeDto.OptionRequest.builder().value("Lifetime").label("Lifetime").sortOrder(4).build()))
                        .build());

                log.info("[Seed] Created 6 product attributes");

                // -- Collections -------------------------------------------
                List<ProductSummaryDTO> products = productService.getAllProducts();
                List<String> allProductIds = products.stream().map(ProductSummaryDTO::getId).toList();

                List<String> electronicsIds = new ArrayList<>();
                for (int i = 0; i < Math.min(5, products.size()); i++) electronicsIds.add(products.get(i).getId());
                for (int i = Math.max(0, products.size() - 4); i < products.size(); i++) {
                    if (!electronicsIds.contains(products.get(i).getId())) electronicsIds.add(products.get(i).getId());
                }

                collectionService.createCollection(CollectionDto.Request.builder()
                        .name("Best Sellers").slug("best-sellers")
                        .description("Our most popular products across all categories")
                        .type("MANUAL").status("ACTIVE")
                        .imageUrl("https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=400")
                        .productIds(allProductIds.subList(0, Math.min(10, allProductIds.size())))
                        .sortOrder(1).isFeatured(true)
                        .build());

                collectionService.createCollection(CollectionDto.Request.builder()
                        .name("New Arrivals").slug("new-arrivals")
                        .description("Latest additions to our store")
                        .type("MANUAL").status("ACTIVE")
                        .imageUrl("https://images.unsplash.com/photo-1472851294608-062f824d29cc?w=400")
                        .productIds(allProductIds.subList(Math.max(0, allProductIds.size() - 8), allProductIds.size()))
                        .sortOrder(2).isFeatured(true)
                        .build());

                collectionService.createCollection(CollectionDto.Request.builder()
                        .name("Tech Essentials").slug("tech-essentials")
                        .description("Must-have technology products for modern life")
                        .type("MANUAL").status("ACTIVE")
                        .imageUrl("https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=400")
                        .productIds(electronicsIds)
                        .sortOrder(3).isFeatured(true)
                        .build());

                collectionService.createCollection(CollectionDto.Request.builder()
                        .name("Under $100").slug("under-100")
                        .description("Great value products under $100")
                        .type("AUTOMATED").status("ACTIVE")
                        .conditions("{\"priceMax\": 100}")
                        .productIds(products.stream()
                                .filter(p -> p.getPrice().compareTo(new BigDecimal("100")) < 0)
                                .map(ProductSummaryDTO::getId).toList())
                        .sortOrder(4).isFeatured(false)
                        .build());

                collectionService.createCollection(CollectionDto.Request.builder()
                        .name("Summer Sale").slug("summer-sale")
                        .description("Hot deals for the summer season")
                        .type("MANUAL").status("DRAFT")
                        .imageUrl("https://images.unsplash.com/photo-1555529669-e69e7aa0ba9a?w=400")
                        .productIds(allProductIds.subList(2, Math.min(8, allProductIds.size())))
                        .sortOrder(5).isFeatured(false)
                        .startDate(Instant.now().plus(30, ChronoUnit.DAYS))
                        .endDate(Instant.now().plus(90, ChronoUnit.DAYS))
                        .build());

                log.info("[Seed] Created 5 collections");

                // -- Reviews (using DB customers) --------------------------
                var dbCustomers = customerRepository.findAll(
                        org.springframework.data.domain.PageRequest.of(0, 20)).getContent();

                String[] reviewTitles = {"Absolutely amazing!", "Great product", "Highly recommend!",
                        "Decent but could improve", "Love it!", "Solid purchase",
                        "Not what I expected", "Exceeded expectations!", "Very good quality", "Outstanding!"};
                String[] reviewContents = {
                        "Best purchase I've made this year. Sound quality is incredible and the battery lasts forever.",
                        "Very good quality, only minor issue is the size is a bit large for my wrist.",
                        "Perfect for my daily use. Build quality is exceptional.",
                        "Good product overall but the instruction manual could be better.",
                        "Exactly what I was looking for. Arrived quickly and works perfectly.",
                        "Great value for the price. Would buy again.",
                        "Quality is okay but doesn't match the product photos.",
                        "Wow, this is amazing quality. My family loves it too.",
                        "Sturdy build and nice design. Shipping was fast.",
                        "Premium quality all around. Worth every penny.",
                };
                int[] ratings = {5, 4, 5, 3, 5, 4, 2, 5, 4, 5};

                int reviewsCreated = 0;
                int customerCount = dbCustomers.size();
                for (int i = 0; i < Math.min(reviewTitles.length, products.size()) && customerCount > 0; i++) {
                    try {
                        var cust = dbCustomers.get(i % customerCount);
                        reviewService.createReview(ReviewDto.Request.builder()
                                .productId(products.get(i).getId())
                                .productName(products.get(i).getName())
                                .customerName(cust.getFirstName() + " " + cust.getLastName())
                                .customerEmail(cust.getEmail())
                                .rating(ratings[i])
                                .title(reviewTitles[i])
                                .content(reviewContents[i])
                                .isVerifiedPurchase(i % 2 == 0)
                                .build());
                        reviewsCreated++;
                    } catch (Exception e) {
                        log.warn("[Seed] Review skipped for product {}: {}", products.get(i).getName(), e.getMessage());
                    }
                }
                // Extra reviews for popular products (first 5 get 2 extra each)
                String[] extraTitles = {"Great product!", "Highly recommend", "Good value", "Very satisfied",
                        "Love this!", "Nice quality", "Perfect gift", "Daily essential", "Impressive", "Must have"};
                String[] extraContent = {
                        "Really happy with this purchase. Works exactly as described.",
                        "Would definitely recommend to friends and family. Top quality.",
                        "Great value for the money. Can't complain about anything.",
                        "Using this daily and it performs flawlessly. Very satisfied.",
                        "Got this as a gift and the recipient absolutely loved it!",
                        "The build quality is outstanding. Very impressed with the finish.",
                        "Bought this for my home office setup and it's perfect.",
                        "Reliable and well-made. Exactly what I needed.",
                        "The design is beautiful and functionality is spot on.",
                        "Everyone should have one of these. Changed my daily routine.",
                };
                int[] extraRatings = {5, 4, 5, 4, 5, 5, 4, 5, 4, 5};

                for (int p = 0; p < Math.min(5, products.size()) && customerCount > 0; p++) {
                    for (int r = 0; r < 2; r++) {
                        int idx = p * 2 + r;
                        try {
                            // Use different customers for extra reviews (offset by 5)
                            var cust = dbCustomers.get((idx + 5) % customerCount);
                            reviewService.createReview(ReviewDto.Request.builder()
                                    .productId(products.get(p).getId())
                                    .productName(products.get(p).getName())
                                    .customerName(cust.getFirstName() + " " + cust.getLastName())
                                    .customerEmail(cust.getEmail())
                                    .rating(extraRatings[idx])
                                    .title(extraTitles[idx])
                                    .content(extraContent[idx])
                                    .isVerifiedPurchase(true)
                                    .build());
                            reviewsCreated++;
                        } catch (Exception e) {
                            log.warn("[Seed] Extra review skipped: {}", e.getMessage());
                        }
                    }
                }
                log.info("[Seed] Created {} reviews", reviewsCreated);

                // -- SEO Metadata ------------------------------------------
                int seoCreated = 0;
                for (int i = 0; i < Math.min(10, products.size()); i++) {
                    ProductSummaryDTO p = products.get(i);
                    try {
                        seoService.upsertSeoForEntity("PRODUCT", p.getId(), SeoDto.Request.builder()
                                .metaTitle(p.getName() + " - Buy Online | Commerce Pro")
                                .metaDescription("Shop " + p.getName() + " at the best price. Free shipping on orders over $50.")
                                .metaKeywords(p.getCategory() != null ? p.getCategory().toLowerCase() : "")
                                .canonicalUrl("/products/" + p.getSku().toLowerCase())
                                .ogTitle(p.getName())
                                .ogDescription("Get " + p.getName() + " at Commerce Pro Store")
                                .ogImage(p.getImage())
                                .robotsDirective("index, follow")
                                .slug(p.getSku().toLowerCase())
                                .build());
                        seoCreated++;
                    } catch (Exception e) {
                        log.warn("[Seed] SEO skipped for product {}: {}", p.getName(), e.getMessage());
                    }
                }
                log.info("[Seed] Created {} SEO entries", seoCreated);

                log.info("[Seed] Catalog seed complete");

            } catch (Exception e) {
                log.error("[Seed] Catalog seed failed: {}", e.getMessage(), e);
            } finally {
                clearSecurityContext();
            }
        };
    }

    // -------------------------------------------------------------------------
    // 5. ORDERS (uses DB customers + products)
    // -------------------------------------------------------------------------

    @Bean
    @Profile("dev")
    @Order(5)
    CommandLineRunner seedOrders(ProductService productService,
                                 OrderService orderService,
                                 com.commerce_pro_backend.customer.repository.CustomerRepository customerRepository,
                                 com.commerce_pro_backend.customer.repository.CustomerAddressRepository customerAddressRepository,
                                 UserDetailsService userDetailsService) {
        return args -> {
            log.info("━━━ [Seed] Orders ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            List<ProductSummaryDTO> products = productService.getAllProducts();
            if (products.isEmpty()) {
                log.warn("[Seed] No products found — skipping order seed");
                return;
            }

            var dbCustomers = customerRepository.findAll(
                    org.springframework.data.domain.PageRequest.of(0, 15)).getContent();
            if (dbCustomers.isEmpty()) {
                log.warn("[Seed] No customers found — skipping order seed");
                return;
            }

            try {
                authenticateAsSuperAdmin(userDetailsService);
                log.info("[Seed] SecurityContext set to superadmin — order seeding can proceed");
            } catch (Exception e) {
                log.error("[Seed] Could not set SecurityContext: {}. Order seeding aborted.", e.getMessage());
                return;
            }

            try {
                int total = products.size();

                // 1) DRAFT
                seedOrderScenario(orderService, "DRAFT", dbCustomers.get(0 % dbCustomers.size()),
                        customerAddressRepository, slice(products, 0, 2, total), null);

                // 2) PENDING_PAYMENT
                seedOrderScenario(orderService, "PENDING_PAYMENT", dbCustomers.get(1 % dbCustomers.size()),
                        customerAddressRepository, slice(products, 1, 2, total), null);

                // 3) CONFIRMED
                seedOrderScenario(orderService, "CONFIRMED", dbCustomers.get(2 % dbCustomers.size()),
                        customerAddressRepository, slice(products, 2, 3, total), "SAVE10");

                // 4) ON_HOLD
                seedOrderScenario(orderService, "ON_HOLD", dbCustomers.get(3 % dbCustomers.size()),
                        customerAddressRepository, slice(products, 3, 2, total), null);

                // 5) PROCESSING
                seedOrderScenario(orderService, "PROCESSING", dbCustomers.get(4 % dbCustomers.size()),
                        customerAddressRepository, slice(products, 4, 3, total), null);

                // 6) SHIPPED
                seedOrderScenario(orderService, "SHIPPED", dbCustomers.get(5 % dbCustomers.size()),
                        customerAddressRepository, slice(products, 5, 2, total), null);

                // 7) DELIVERED
                seedOrderScenario(orderService, "DELIVERED", dbCustomers.get(6 % dbCustomers.size()),
                        customerAddressRepository, slice(products, 6, 2, total), null);

                // 8) CLOSED
                seedOrderScenario(orderService, "CLOSED", dbCustomers.get(7 % dbCustomers.size()),
                        customerAddressRepository, slice(products, 7, 3, total), "WELCOME15");

                // 9) CANCELLED
                seedOrderScenario(orderService, "CANCELLED", dbCustomers.get(8 % dbCustomers.size()),
                        customerAddressRepository, slice(products, 8, 1, total), null);

                // 10) HIGH_VALUE
                seedOrderScenario(orderService, "HIGH_VALUE", dbCustomers.get(9 % dbCustomers.size()),
                        customerAddressRepository, slice(products, 9, 4, total), null);

                log.info("[Seed] Orders complete — 10 scenario orders created");

                // ── Update customer stats from created orders ──────────────────
                updateCustomerStatsFromOrders(orderService, customerRepository, dbCustomers);
                log.info("[Seed] Customer stats (totalOrders, lifetimeSpend, lastOrder) updated");

            } finally {
                clearSecurityContext();
                log.info("[Seed] SecurityContext cleared after order seeding");
            }
        };
    }

    private void seedOrderScenario(OrderService orderService, String scenario,
                                    com.commerce_pro_backend.customer.entity.Customer customer,
                                    com.commerce_pro_backend.customer.repository.CustomerAddressRepository addressRepo,
                                    List<ProductSummaryDTO> products, String coupon) {
        try {
            String name = customer.getFirstName() + " " + customer.getLastName();

            // Build address from customer's actual DB addresses
            var dbAddresses = addressRepo.findByCustomerId(customer.getId());
            OrderAddressDTO shippingAddress;
            OrderAddressDTO billingAddress;

            if (!dbAddresses.isEmpty()) {
                // Use default address or first address for shipping
                var shipAddr = dbAddresses.stream()
                        .filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                        .findFirst().orElse(dbAddresses.get(0));
                shippingAddress = toOrderAddress(shipAddr);

                // Use a BILLING-type address if available, else same as shipping
                var billAddr = dbAddresses.stream()
                        .filter(a -> a.getAddressType() == com.commerce_pro_backend.customer.enums.AddressType.BILLING
                                  || a.getAddressType() == com.commerce_pro_backend.customer.enums.AddressType.BOTH)
                        .findFirst().orElse(shipAddr);
                billingAddress = toOrderAddress(billAddr);
            } else {
                // Fallback — should not happen if customer seed ran first
                shippingAddress = OrderAddressDTO.builder()
                        .fullName(name).addressLine1("123 Main St").city("New York")
                        .state("NY").postalCode("10001").country("USA").build();
                billingAddress = shippingAddress;
            }

            OrderSource source = switch (scenario) {
                case "ON_HOLD" -> OrderSource.API;
                case "CLOSED" -> OrderSource.MANUAL;
                case "HIGH_VALUE" -> OrderSource.API;
                default -> OrderSource.STOREFRONT;
            };
            BigDecimal shipping = switch (scenario) {
                case "CONFIRMED", "CLOSED" -> new BigDecimal("12.99");
                case "HIGH_VALUE" -> new BigDecimal("49.99");
                default -> new BigDecimal("7.99");
            };
            BigDecimal discount = coupon != null ? new BigDecimal("10.00") : BigDecimal.ZERO;
            String shippingMethod = switch (scenario) {
                case "CONFIRMED", "CLOSED" -> "Priority Shipping";
                case "SHIPPED", "DELIVERED" -> "Express Shipping";
                default -> "Standard Shipping";
            };

            List<OrderItemRequestDTO> items;
            if ("HIGH_VALUE".equals(scenario)) {
                items = new ArrayList<>();
                for (ProductSummaryDTO p : products) {
                    items.add(OrderItemRequestDTO.builder()
                            .productId(p.getId()).quantity(3)
                            .taxRate(new BigDecimal("0.08")).itemDiscount(BigDecimal.ZERO).build());
                }
            } else {
                items = buildItems(products, 0, products.size());
            }

            var response = orderService.createOrder(CreateOrderRequestDTO.builder()
                    .customerId(customer.getId())
                    .customerName(name)
                    .customerEmail(customer.getEmail())
                    .customerPhone(customer.getPhone())
                    .source(source)
                    .shippingAddress(shippingAddress)
                    .billingAddress(billingAddress)
                    .shippingCost(shipping)
                    .discountAmount(discount)
                    .shippingMethod(shippingMethod)
                    .couponCode(coupon)
                    .currency("USD")
                    .customerNotes("HIGH_VALUE".equals(scenario) ? "Priority enterprise delivery required" : null)
                    .internalNotes("HIGH_VALUE".equals(scenario) ? "Auto-flagged for risk review — high-value order" : null)
                    .items(items)
                    .build());

            // Progress the order through its lifecycle
            switch (scenario) {
                case "CONFIRMED" -> orderService.confirmOrder(response.getId());
                case "ON_HOLD" -> {
                    orderService.confirmOrder(response.getId());
                    OrderHoldRequest holdReq = new OrderHoldRequest();
                    holdReq.setReason("Billing address verification required");
                    orderService.holdOrder(response.getId(), holdReq);
                }
                case "PROCESSING" -> {
                    orderService.confirmOrder(response.getId());
                    orderService.markProcessing(response.getId());
                }
                case "SHIPPED" -> {
                    orderService.confirmOrder(response.getId());
                    orderService.markProcessing(response.getId());
                    TrackingUpdateRequest t = new TrackingUpdateRequest();
                    t.setTrackingNumber("UPS1234567890");
                    t.setCarrier("UPS");
                    orderService.markShipped(response.getId(), t);
                }
                case "DELIVERED" -> {
                    orderService.confirmOrder(response.getId());
                    orderService.markProcessing(response.getId());
                    TrackingUpdateRequest t = new TrackingUpdateRequest();
                    t.setTrackingNumber("FEDEX9988776655");
                    t.setCarrier("FedEx");
                    orderService.markShipped(response.getId(), t);
                    orderService.markDelivered(response.getId());
                }
                case "CLOSED" -> {
                    orderService.confirmOrder(response.getId());
                    orderService.markProcessing(response.getId());
                    TrackingUpdateRequest t = new TrackingUpdateRequest();
                    t.setTrackingNumber("DHL4455667788");
                    t.setCarrier("DHL");
                    orderService.markShipped(response.getId(), t);
                    orderService.markDelivered(response.getId());
                    orderService.closeOrder(response.getId());
                }
                case "CANCELLED" -> {
                    OrderCancelRequest cancelReq = new OrderCancelRequest();
                    cancelReq.setReason("Customer requested cancellation — changed mind");
                    orderService.cancelOrder(response.getId(), cancelReq);
                }
                default -> {} // DRAFT, PENDING_PAYMENT, HIGH_VALUE stay as-is
            }

            log.info("[Seed] {} order created — {} ({})", scenario, name, response.getOrderNumber());
        } catch (Exception e) {
            log.error("[Seed] {} order failed: {}", scenario, e.getMessage());
        }
    }

    private OrderAddressDTO toOrderAddress(com.commerce_pro_backend.customer.entity.CustomerAddress addr) {
        return OrderAddressDTO.builder()
                .fullName(addr.getFullName())
                .addressLine1(addr.getAddressLine1())
                .addressLine2(addr.getAddressLine2())
                .city(addr.getCity())
                .state(addr.getState())
                .postalCode(addr.getPostalCode())
                .country(addr.getCountry())
                .phone(addr.getPhone())
                .build();
    }

    private void updateCustomerStatsFromOrders(
            OrderService orderService,
            com.commerce_pro_backend.customer.repository.CustomerRepository customerRepository,
            List<com.commerce_pro_backend.customer.entity.Customer> customers) {

        var orderFilter = new OrderFilterDTO();
        var allOrders = orderService.listOrders(orderFilter,
                org.springframework.data.domain.PageRequest.of(0, 100)).getContent();

        for (var customer : customers) {
            var custOrders = allOrders.stream()
                    .filter(o -> customer.getId().equals(o.getCustomerId()))
                    .toList();

            if (custOrders.isEmpty()) continue;

            customer.setTotalOrders(custOrders.size());
            BigDecimal totalSpend = custOrders.stream()
                    .map(OrderSummaryDTO::getTotalAmount)
                    .filter(a -> a != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            customer.setLifetimeSpend(totalSpend);
            customer.recalculateAverageOrderValue();

            // Find the most recent order
            custOrders.stream()
                    .filter(o -> o.getCreatedAt() != null)
                    .max(java.util.Comparator.comparing(OrderSummaryDTO::getCreatedAt))
                    .ifPresent(latest -> {
                        customer.setLastOrderAt(latest.getCreatedAt());
                        customer.setLastOrderId(latest.getId());
                    });

            // Re-evaluate tier based on updated stats
            customer.setTier(customer.evaluateTier());

            customerRepository.save(customer);
        }
    }

    // -------------------------------------------------------------------------
    // 6. FULFILLMENT (carriers, pick lists, shipments)
    // -------------------------------------------------------------------------

    @Bean
    @Profile("dev")
    @Order(6)
    CommandLineRunner seedFulfillment(
            com.commerce_pro_backend.fulfillment.service.FulfillmentService fulfillmentService,
            com.commerce_pro_backend.fulfillment.service.ShipmentService shipmentService,
            com.commerce_pro_backend.order.repository.OrderRepository orderRepository,
            UserDetailsService userDetailsService) {
        return args -> {
            log.info("━━━ [Seed] Fulfillment ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            try {
                authenticateAsSuperAdmin(userDetailsService);

                // -- Carriers ----------------------------------------------
                com.commerce_pro_backend.fulfillment.dto.CarrierRequestDTO manual =
                        new com.commerce_pro_backend.fulfillment.dto.CarrierRequestDTO();
                manual.setName("Manual / In-House");
                manual.setCode("MANUAL");
                manual.setNotes("Default in-house delivery — no external carrier");
                manual.setIsDefault(true);
                com.commerce_pro_backend.fulfillment.dto.CarrierDTO manualCarrier =
                        fulfillmentService.createCarrier(manual);

                com.commerce_pro_backend.fulfillment.dto.CarrierRequestDTO fedex =
                        new com.commerce_pro_backend.fulfillment.dto.CarrierRequestDTO();
                fedex.setName("FedEx");
                fedex.setCode("FEDEX");
                fedex.setTrackingUrlTemplate("https://www.fedex.com/apps/fedextrack/?tracknumbers={trackingNumber}");
                fedex.setIsDefault(false);
                fulfillmentService.createCarrier(fedex);

                com.commerce_pro_backend.fulfillment.dto.CarrierRequestDTO ups =
                        new com.commerce_pro_backend.fulfillment.dto.CarrierRequestDTO();
                ups.setName("UPS");
                ups.setCode("UPS");
                ups.setTrackingUrlTemplate("https://www.ups.com/track?tracknum={trackingNumber}");
                ups.setIsDefault(false);
                fulfillmentService.createCarrier(ups);

                com.commerce_pro_backend.fulfillment.dto.CarrierRequestDTO dhl =
                        new com.commerce_pro_backend.fulfillment.dto.CarrierRequestDTO();
                dhl.setName("DHL Express");
                dhl.setCode("DHL");
                dhl.setTrackingUrlTemplate("https://www.dhl.com/en/express/tracking.html?AWB={trackingNumber}");
                dhl.setIsDefault(false);
                fulfillmentService.createCarrier(dhl);

                log.info("[Seed] Created 4 carriers");

                // -- Shipping Rules ----------------------------------------
                com.commerce_pro_backend.fulfillment.dto.ShippingRuleRequestDTO standardRule =
                        new com.commerce_pro_backend.fulfillment.dto.ShippingRuleRequestDTO();
                standardRule.setName("Standard Domestic Shipping");
                standardRule.setConditionType("ALWAYS");
                standardRule.setShippingMethod("Standard");
                standardRule.setServiceLevel("3-5 Business Days");
                standardRule.setPriority(100);
                standardRule.setIsActive(true);
                fulfillmentService.createRule(standardRule);

                com.commerce_pro_backend.fulfillment.dto.ShippingRuleRequestDTO expressRule =
                        new com.commerce_pro_backend.fulfillment.dto.ShippingRuleRequestDTO();
                expressRule.setName("Express Shipping — Orders over $100");
                expressRule.setConditionType("PRICE");
                expressRule.setConditionMin(new BigDecimal("100.00"));
                expressRule.setShippingMethod("Express");
                expressRule.setServiceLevel("1-2 Business Days");
                expressRule.setPriority(50);
                expressRule.setIsActive(true);
                fulfillmentService.createRule(expressRule);

                com.commerce_pro_backend.fulfillment.dto.ShippingRuleRequestDTO freeShipping =
                        new com.commerce_pro_backend.fulfillment.dto.ShippingRuleRequestDTO();
                freeShipping.setName("Free Shipping — Orders over $200");
                freeShipping.setConditionType("PRICE");
                freeShipping.setConditionMin(new BigDecimal("200.00"));
                freeShipping.setShippingMethod("Free Shipping");
                freeShipping.setServiceLevel("5-7 Business Days");
                freeShipping.setPriority(200);
                freeShipping.setIsActive(true);
                fulfillmentService.createRule(freeShipping);

                log.info("[Seed] Created 3 shipping rules");

                // -- Pick List — from CONFIRMED orders ---------------------
                java.util.List<com.commerce_pro_backend.order.entity.Order> confirmedOrders =
                        orderRepository.findByStatus(com.commerce_pro_backend.order.enums.OrderStatus.CONFIRMED);
                if (!confirmedOrders.isEmpty()) {
                    java.util.List<String> pickOrderIds = confirmedOrders.stream()
                            .limit(2)
                            .map(com.commerce_pro_backend.order.entity.Order::getId)
                            .collect(java.util.stream.Collectors.toList());

                    com.commerce_pro_backend.fulfillment.dto.CreatePickListRequest pickReq =
                            new com.commerce_pro_backend.fulfillment.dto.CreatePickListRequest();
                    pickReq.setOrderIds(pickOrderIds);
                    pickReq.setWarehouseName("Main Warehouse");
                    pickReq.setNotes("Seeded pick list for demo purposes");
                    com.commerce_pro_backend.fulfillment.dto.PickListDTO pickList =
                            fulfillmentService.generatePickList(pickReq);
                    log.info("[Seed] Generated pick list {} for {} orders",
                            pickList.getPickListNumber(), pickOrderIds.size());
                }

                // -- Shipment — from SHIPPED orders ------------------------
                java.util.List<com.commerce_pro_backend.order.entity.Order> shippedOrders =
                        orderRepository.findByStatus(com.commerce_pro_backend.order.enums.OrderStatus.SHIPPED);
                if (!shippedOrders.isEmpty()) {
                    com.commerce_pro_backend.order.entity.Order shippedOrder = shippedOrders.get(0);
                    com.commerce_pro_backend.fulfillment.dto.CreateShipmentRequest shipReq =
                            new com.commerce_pro_backend.fulfillment.dto.CreateShipmentRequest();
                    shipReq.setOrderId(shippedOrder.getId());
                    shipReq.setCarrierId(manualCarrier.getId());
                    shipReq.setTrackingNumber("TRK-DEMO-001");
                    shipReq.setShippingMethod("Standard");
                    shipReq.setEstimatedDeliveryDate(java.time.LocalDate.now().plusDays(3));
                    shipReq.setNotes("Seeded shipment for demo purposes");
                    com.commerce_pro_backend.fulfillment.dto.ShipmentDTO shipment =
                            shipmentService.createShipment(shipReq);

                    com.commerce_pro_backend.fulfillment.dto.AddTrackingEventRequest eventReq =
                            new com.commerce_pro_backend.fulfillment.dto.AddTrackingEventRequest();
                    eventReq.setStatus("IN_TRANSIT");
                    eventReq.setDescription("Package departed sorting facility");
                    eventReq.setLocation("Distribution Center, Chicago IL");
                    shipmentService.addTrackingEvent(shipment.getId(), eventReq);
                    log.info("[Seed] Created shipment {} with tracking event", shipment.getShipmentNumber());
                }

                log.info("[Seed] Fulfillment seed complete");

            } catch (Exception e) {
                log.error("[Seed] Fulfillment seed failed: {}", e.getMessage(), e);
            } finally {
                clearSecurityContext();
            }
        };
    }

    // -------------------------------------------------------------------------
    // 7. INVENTORY EXTENDED (adjustments, transfers, forecasts, valuations)
    // -------------------------------------------------------------------------

    @Bean
    @Profile("dev")
    @Order(7)
    CommandLineRunner seedInventoryExtended(
            InventoryService inventoryService,
            DemandForecastService forecastService,
            InventoryValuationService valuationService,
            ProductService productService,
            UserDetailsService userDetailsService) {
        return args -> {
            log.info("━━━ [Seed] Inventory Extended ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            try {
                authenticateAsSuperAdmin(userDetailsService);

                List<ProductSummaryDTO> products = productService.getAllProducts();
                if (products.isEmpty()) {
                    log.warn("[Seed] No products — skipping inventory extended seed");
                    return;
                }

                var inventoryPage = inventoryService.getInventory(new InventoryFilterDTO(),
                        org.springframework.data.domain.PageRequest.of(0, 100));
                var inventoryList = inventoryPage.getContent();

                if (inventoryList.isEmpty()) {
                    log.warn("[Seed] No inventory records — skipping extended seed");
                    return;
                }

                // -- Stock Adjustments -------------------------------------
                String[][] adjustments = {
                    {"Cycle count correction", "Discrepancy found during cycle count", "CYCLE_COUNT"},
                    {"Damaged goods write-off", "3 units damaged in transit", "DAMAGE"},
                    {"Quality control rejection", "Failed QC inspection", "QC_REJECT"},
                    {"Promotional allocation", "Reserved for upcoming sale event", "PROMO"},
                    {"Return to stock", "Customer return - item inspected and restocked", "RETURN"},
                };

                int adjustmentsCreated = 0;
                for (int i = 0; i < Math.min(adjustments.length, inventoryList.size()); i++) {
                    try {
                        int qty = (i % 2 == 0) ? -(i + 1) : (i + 2);
                        inventoryService.adjustStock(inventoryList.get(i).getId(),
                                StockUpdateRequestDTO.builder()
                                        .quantity(Math.abs(qty))
                                        .adjust(qty > 0)
                                        .reason(adjustments[i][0])
                                        .notes(adjustments[i][1])
                                        .reference("ADJ-2024-" + String.format("%03d", i + 1))
                                        .referenceType(adjustments[i][2])
                                        .build());
                        adjustmentsCreated++;
                    } catch (Exception e) {
                        log.warn("[Seed] Adjustment skipped: {}", e.getMessage());
                    }
                }
                log.info("[Seed] Created {} stock adjustments", adjustmentsCreated);

                // -- Stock Transfers ---------------------------------------
                var warehouses = inventoryService.getAllWarehouses();
                if (warehouses.size() >= 2) {
                    String fromWarehouse = warehouses.get(0).getId();
                    String toWarehouse = warehouses.get(1).getId();

                    int transfersCreated = 0;
                    for (int i = 0; i < Math.min(3, products.size()); i++) {
                        try {
                            inventoryService.transferStock(StockTransferRequestDTO.builder()
                                    .fromWarehouseId(fromWarehouse)
                                    .toWarehouseId(toWarehouse)
                                    .productId(products.get(i).getId())
                                    .quantity(5 + i * 2)
                                    .notes("Rebalancing inventory between warehouses")
                                    .reference("TRF-2024-" + String.format("%03d", i + 1))
                                    .build());
                            transfersCreated++;
                        } catch (Exception e) {
                            log.warn("[Seed] Transfer skipped: {}", e.getMessage());
                        }
                    }
                    log.info("[Seed] Created {} stock transfers", transfersCreated);
                }

                // -- Demand Forecasts --------------------------------------
                Instant now = Instant.now();
                String[] periods = {"monthly", "weekly", "quarterly"};
                String[] algorithms = {"MOVING_AVERAGE", "EXPONENTIAL_SMOOTHING", "LINEAR_REGRESSION"};

                int forecastsCreated = 0;
                for (int i = 0; i < Math.min(8, products.size()); i++) {
                    try {
                        ProductSummaryDTO p = products.get(i);
                        int baseDemand = 20 + (int)(Math.random() * 80);

                        forecastService.createForecast(DemandForecastDto.Request.builder()
                                .productId(p.getId())
                                .productName(p.getName())
                                .sku(p.getSku())
                                .period(periods[i % 3])
                                .algorithm(algorithms[i % 3])
                                .status("active")
                                .startDate(now.minus(30, ChronoUnit.DAYS))
                                .endDate(now.plus(60, ChronoUnit.DAYS))
                                .totalPredictedDemand(baseDemand * 30)
                                .averageDailyDemand((double) baseDemand)
                                .peakDemandQuantity((int)(baseDemand * 1.5))
                                .safetyStockRecommendation((int)(baseDemand * 0.3))
                                .reorderPointRecommendation((int)(baseDemand * 0.5))
                                .generatedBy("system")
                                .build());
                        forecastsCreated++;
                    } catch (Exception e) {
                        log.warn("[Seed] Forecast skipped for {}: {}", products.get(i).getName(), e.getMessage());
                    }
                }
                log.info("[Seed] Created {} demand forecasts", forecastsCreated);

                // -- Inventory Valuations ----------------------------------
                String[] valuationMethods = {"fifo", "lifo", "weighted_average", "fifo"};
                String warehouseId = warehouses.isEmpty() ? null : warehouses.get(0).getId();
                String warehouseName = warehouses.isEmpty() ? "Main Warehouse" : warehouses.get(0).getName();

                int valuationsCreated = 0;
                for (int i = 0; i < Math.min(8, products.size()); i++) {
                    try {
                        ProductSummaryDTO p = products.get(i);
                        BigDecimal cost = unitCost(p.getPrice());
                        int qty = 50 + (int)(Math.random() * 150);

                        List<InventoryValuationDto.CostLayerRequest> layers = List.of(
                                InventoryValuationDto.CostLayerRequest.builder()
                                        .receiptDate(now.minus(60, ChronoUnit.DAYS))
                                        .unitCost(cost.multiply(new BigDecimal("0.95")))
                                        .originalQuantity((int)(qty * 0.6))
                                        .reference("PO-2024-" + String.format("%03d", i * 2 + 1))
                                        .build(),
                                InventoryValuationDto.CostLayerRequest.builder()
                                        .receiptDate(now.minus(15, ChronoUnit.DAYS))
                                        .unitCost(cost)
                                        .originalQuantity((int)(qty * 0.4))
                                        .reference("PO-2024-" + String.format("%03d", i * 2 + 2))
                                        .build()
                        );

                        valuationService.createValuation(InventoryValuationDto.Request.builder()
                                .productId(p.getId())
                                .productName(p.getName())
                                .sku(p.getSku())
                                .warehouseId(warehouseId)
                                .warehouseName(warehouseName)
                                .valuationMethod(valuationMethods[i % 4])
                                .status("active")
                                .totalQuantity(qty)
                                .averageUnitCost(cost)
                                .totalValue(cost.multiply(BigDecimal.valueOf(qty)))
                                .calculatedBy("system")
                                .costLayers(layers)
                                .build());
                        valuationsCreated++;
                    } catch (Exception e) {
                        log.warn("[Seed] Valuation skipped for {}: {}", products.get(i).getName(), e.getMessage());
                    }
                }
                log.info("[Seed] Created {} inventory valuations", valuationsCreated);

                log.info("[Seed] Inventory extended seed complete");

            } catch (Exception e) {
                log.error("[Seed] Inventory extended seed failed: {}", e.getMessage(), e);
            } finally {
                clearSecurityContext();
            }
        };
    }

    // -------------------------------------------------------------------------
    // 8. NOTIFICATIONS (references actual DB orders, products, customers)
    // -------------------------------------------------------------------------

    @Bean
    @Profile("dev")
    @Order(8)
    CommandLineRunner seedNotifications(
            NotificationService notificationService,
            UserRepository userRepository,
            OrderService orderService,
            ProductService productService,
            InventoryService inventoryService,
            UserDetailsService userDetailsService) {
        return args -> {
            log.info("━━━ [Seed] Notifications ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            try {
                authenticateAsSuperAdmin(userDetailsService);

                var superadmin = userRepository.findByUsername("superadmin");
                if (superadmin.isEmpty()) {
                    log.warn("[Seed] No superadmin user — skipping notification seed");
                    return;
                }
                String userId = superadmin.get().getId();

                // Fetch actual data from DB for realistic notifications
                var orders = orderService.listOrders(new OrderFilterDTO(),
                        org.springframework.data.domain.PageRequest.of(0, 10));
                var products = productService.getAllProducts();
                var warehouses = inventoryService.getAllWarehouses();
                String warehouseName = warehouses.isEmpty() ? "Main Warehouse" : warehouses.get(0).getName();

                int created = 0;

                // Order-based notifications (from actual orders)
                if (!orders.getContent().isEmpty()) {
                    var firstOrder = orders.getContent().get(0);
                    createNotif(notificationService, userId,
                            "New Order Received",
                            "Order " + firstOrder.getOrderNumber() + " from " + firstOrder.getCustomerName() + " — $" + firstOrder.getTotalAmount(),
                            NotificationType.ORDER_UPDATE, NotificationPriority.NORMAL, "/orders");
                    created++;

                    if (orders.getContent().size() > 2) {
                        var shippedOrder = orders.getContent().get(2);
                        createNotif(notificationService, userId,
                                "Order Shipped",
                                "Order " + shippedOrder.getOrderNumber() + " shipped via UPS — tracking: UPS1234567890",
                                NotificationType.ORDER_UPDATE, NotificationPriority.NORMAL, "/orders");
                        created++;
                    }

                    if (orders.getContent().size() > 5) {
                        var cancelledOrder = orders.getContent().stream()
                                .filter(o -> "CANCELLED".equals(o.getStatus()))
                                .findFirst().orElse(orders.getContent().get(5));
                        createNotif(notificationService, userId,
                                "Order Cancelled",
                                "Order " + cancelledOrder.getOrderNumber() + " cancelled by " + cancelledOrder.getCustomerName(),
                                NotificationType.ORDER_UPDATE, NotificationPriority.NORMAL, "/orders");
                        created++;
                    }

                    // High-value order notification
                    orders.getContent().stream()
                            .filter(o -> o.getTotalAmount() != null && o.getTotalAmount().compareTo(new BigDecimal("500")) > 0)
                            .findFirst()
                            .ifPresent(hvOrder -> createNotif(notificationService, userId,
                                    "High-Value Order Flagged",
                                    "Order " + hvOrder.getOrderNumber() + " from " + hvOrder.getCustomerName() + " ($" + hvOrder.getTotalAmount() + ") auto-flagged for risk review",
                                    NotificationType.ORDER_UPDATE, NotificationPriority.HIGH, "/orders"));
                    created++;
                }

                // Product/inventory-based notifications
                if (!products.isEmpty()) {
                    var lowStockProduct = products.stream()
                            .min((a, b) -> Integer.compare(a.getStock(), b.getStock()))
                            .get();
                    createNotif(notificationService, userId,
                            "Low Stock Alert",
                            lowStockProduct.getName() + " — only " + lowStockProduct.getStock() + " units remaining in " + warehouseName,
                            NotificationType.INVENTORY_ALERT, NotificationPriority.HIGH, "/inventory");
                    created++;

                    createNotif(notificationService, userId,
                            "Demand Forecast Alert",
                            "Predicted high demand for " + products.get(0).getName() + " — consider increasing stock by 40%",
                            NotificationType.INVENTORY_ALERT, NotificationPriority.HIGH, "/inventory/forecasting");
                    created++;
                }

                // Stock transfer notification (from DB warehouses)
                if (warehouses.size() >= 2) {
                    createNotif(notificationService, userId,
                            "Stock Transfer Complete",
                            "Stock transfer completed from " + warehouses.get(0).getName() + " to " + warehouses.get(1).getName(),
                            NotificationType.INVENTORY_ALERT, NotificationPriority.LOW, "/inventory");
                    created++;
                }

                // System & analytics notifications
                createNotif(notificationService, userId,
                        "System Update",
                        "Commerce Pro v2.1.0 deployed successfully. New features: analytics dashboard and customer segmentation.",
                        NotificationType.SYSTEM, NotificationPriority.LOW, null);
                created++;

                createNotif(notificationService, userId,
                        "Daily Sales Report Ready",
                        "Your daily sales report for today is ready. Total orders: " + orders.getTotalElements(),
                        NotificationType.REPORT_READY, NotificationPriority.NORMAL, "/analytics");
                created++;

                createNotif(notificationService, userId,
                        "Weekly Analytics Summary",
                        "This week: " + orders.getTotalElements() + " orders, " + products.size() + " active products across " + warehouses.size() + " warehouses",
                        NotificationType.REPORT_READY, NotificationPriority.NORMAL, "/analytics");
                created++;

                createNotif(notificationService, userId,
                        "Security Alert",
                        "Unusual login pattern detected — 3 failed attempts from IP 192.168.1.100",
                        NotificationType.SECURITY, NotificationPriority.URGENT, "/identity/audit");
                created++;

                createNotif(notificationService, userId,
                        "Password Policy Update",
                        "Password policy has been updated: minimum 12 characters now required",
                        NotificationType.SECURITY, NotificationPriority.NORMAL, "/identity/config");
                created++;

                createNotif(notificationService, userId,
                        "New Customer Registration",
                        "New customer registered via Storefront. Total active customers growing.",
                        NotificationType.CUSTOMER_UPDATE, NotificationPriority.LOW, "/customers");
                created++;

                createNotif(notificationService, userId,
                        "Inventory Valuation Updated",
                        "Monthly inventory valuation complete across " + warehouses.size() + " warehouse(s)",
                        NotificationType.INVENTORY_ALERT, NotificationPriority.LOW, "/inventory/valuation");
                created++;

                // Review notification from actual product
                if (!products.isEmpty()) {
                    createNotif(notificationService, userId,
                            "New Review Posted",
                            "A new 5-star review was posted on " + products.get(0).getName(),
                            NotificationType.CUSTOMER_UPDATE, NotificationPriority.LOW, "/reviews");
                    created++;
                }

                // Mark some as read
                var allNotifications = notificationService.getUnreadNotifications(userId);
                for (int i = 0; i < Math.min(5, allNotifications.size()); i++) {
                    try {
                        notificationService.markAsRead(allNotifications.get(i).getId());
                    } catch (Exception ignored) {}
                }

                log.info("[Seed] Notifications complete — {} created ({} marked as read)", created, Math.min(5, created));

            } catch (Exception e) {
                log.error("[Seed] Notification seed failed: {}", e.getMessage(), e);
            } finally {
                clearSecurityContext();
            }
        };
    }

    private void createNotif(NotificationService svc, String userId,
                              String title, String message,
                              NotificationType type, NotificationPriority priority, String link) {
        try {
            svc.createNotification(CreateNotificationRequest.builder()
                    .userId(userId).title(title).message(message)
                    .type(type).channel(NotificationChannel.IN_APP)
                    .priority(priority).link(link).build());
        } catch (Exception e) {
            log.warn("[Seed] Notification skipped: {}", e.getMessage());
        }
    }

    // =====================================================================
    // SECURITY CONTEXT HELPERS
    // =====================================================================

    private void authenticateAsSuperAdmin(UserDetailsService userDetailsService) {
        var userDetails = userDetailsService.loadUserByUsername("superadmin");
        var auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // =====================================================================
    // PRODUCT CATALOGUE
    // =====================================================================

    private List<ProductRequestDTO> buildProductCatalogue() {
        return List.of(

                // -- Electronics ------------------------------------------
                productWithVariants(
                        "Wireless Bluetooth Headphones Pro", "ELEC-HP-001",
                        "Electronics", "Sony",
                        new BigDecimal("199.99"), new BigDecimal("249.99"), 150,
                        "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&h=800&fit=crop",
                        List.of(
                                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&h=800&fit=crop",
                                "https://images.unsplash.com/photo-1484704849700-f032a568e944?w=800&h=800&fit=crop"),
                        "Premium noise-cancelling wireless headphones with 30-hour battery life and superior sound quality.",
                        List.of("electronics", "audio", "wireless", "headphones", "premium"),
                        List.of(ProductAttributeDTO.builder().name("Color")
                                .values(List.of("Black", "Silver", "Blue")).displayOrder(1).build()),
                        List.of(ProductVariantDTO.builder().name("Color")
                                .options(List.of("Black", "Silver", "Blue")).build())),

                product("Smart Watch Series 8", "ELEC-SW-002",
                        "Electronics", "TechBrand",
                        new BigDecimal("399.99"), new BigDecimal("499.99"), 75,
                        "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&h=800&fit=crop",
                        List.of(
                                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&h=800&fit=crop",
                                "https://images.unsplash.com/photo-1579586337278-3befd40fd17a?w=800&h=800&fit=crop"),
                        "Advanced fitness tracking, heart rate monitoring, and seamless smartphone integration.",
                        List.of("electronics", "wearable", "smartwatch", "fitness", "tech")),

                product("Mechanical Gaming Keyboard RGB", "ELEC-KB-003",
                        "Electronics", "KeyChamp",
                        new BigDecimal("149.99"), new BigDecimal("199.99"), 60,
                        "https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?w=800&h=800&fit=crop",
                        List.of(
                                "https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?w=800&h=800&fit=crop",
                                "https://images.unsplash.com/photo-1595225476474-87563907a212?w=800&h=800&fit=crop"),
                        "Cherry MX switches, customizable RGB lighting, and aircraft-grade aluminum frame.",
                        List.of("electronics", "gaming", "keyboard", "mechanical", "rgb")),

                product("4K Ultra HD Webcam", "ELEC-WC-004",
                        "Electronics", "LogiTech",
                        new BigDecimal("129.99"), new BigDecimal("169.99"), 200,
                        "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800&h=800&fit=crop"),
                        "Crystal clear 4K video with auto-focus and noise-reducing microphones.",
                        List.of("electronics", "webcam", "streaming", "4k", "video")),

                product("Portable Bluetooth Speaker", "ELEC-SP-005",
                        "Electronics", "BoseAudio",
                        new BigDecimal("79.99"), new BigDecimal("99.99"), 180,
                        "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=800&h=800&fit=crop",
                        List.of(
                                "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=800&h=800&fit=crop",
                                "https://images.unsplash.com/photo-1545454675-3531b543be5d?w=800&h=800&fit=crop"),
                        "Waterproof portable speaker with 360-degree sound and 12-hour battery.",
                        List.of("electronics", "speaker", "bluetooth", "portable", "audio")),

                // -- Fashion ----------------------------------------------
                product("Classic Leather Messenger Bag", "FASH-BG-006",
                        "Fashion", "Heritage Leather",
                        new BigDecimal("189.99"), new BigDecimal("249.99"), 45,
                        "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&h=800&fit=crop",
                        List.of(
                                "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&h=800&fit=crop",
                                "https://images.unsplash.com/photo-1548036328-c9fa89d128fa?w=800&h=800&fit=crop"),
                        "Hand-stitched full-grain leather messenger bag with brass hardware and padded laptop compartment.",
                        List.of("fashion", "leather", "bag", "messenger", "premium")),

                product("Designer Polarized Sunglasses", "FASH-SG-007",
                        "Fashion", "RayStyle",
                        new BigDecimal("159.99"), new BigDecimal("199.99"), 90,
                        "https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=800&h=800&fit=crop"),
                        "Italian acetate frame with polarized UV400 lenses and spring hinges.",
                        List.of("fashion", "sunglasses", "designer", "polarized", "uv")),

                product("Minimalist Leather Wallet", "FASH-WL-008",
                        "Fashion", "LeatherCraft",
                        new BigDecimal("49.99"), new BigDecimal("69.99"), 120,
                        "https://images.unsplash.com/photo-1627123424574-724758594e93?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1627123424574-724758594e93?w=800&h=800&fit=crop"),
                        "Slim bifold wallet with RFID protection and 6 card slots.",
                        List.of("fashion", "wallet", "leather", "minimalist", "rfid")),

                product("Urban Canvas Sneakers", "FASH-SN-009",
                        "Fashion", "StreetWear",
                        new BigDecimal("89.99"), new BigDecimal("119.99"), 95,
                        "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&h=800&fit=crop",
                        List.of(
                                "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&h=800&fit=crop",
                                "https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?w=800&h=800&fit=crop"),
                        "Eco-friendly canvas upper with memory-foam insole and vulcanized rubber outsole.",
                        List.of("fashion", "sneakers", "canvas", "urban", "eco")),

                product("Automatic Chronograph Watch", "FASH-WT-010",
                        "Fashion", "TimePiece",
                        new BigDecimal("299.99"), new BigDecimal("399.99"), 30,
                        "https://images.unsplash.com/photo-1524592094714-0f0654e20314?w=800&h=800&fit=crop",
                        List.of(
                                "https://images.unsplash.com/photo-1524592094714-0f0654e20314?w=800&h=800&fit=crop",
                                "https://images.unsplash.com/photo-1522312346375-d1a52e2b99b3?w=800&h=800&fit=crop"),
                        "Swiss automatic movement, sapphire crystal, and genuine leather strap.",
                        List.of("fashion", "watch", "automatic", "chronograph", "luxury")),

                // -- Home & Kitchen ---------------------------------------
                product("Pour-Over Coffee Maker Set", "HOME-CF-011",
                        "Home & Kitchen", "BrewMaster",
                        new BigDecimal("59.99"), new BigDecimal("79.99"), 100,
                        "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=800&h=800&fit=crop"),
                        "Borosilicate glass carafe, stainless steel filter, and bamboo collar.",
                        List.of("home", "coffee", "kitchen", "pour-over", "manual")),

                product("Insulated Water Bottle 1L", "HOME-WB-012",
                        "Home & Kitchen", "HydroLife",
                        new BigDecimal("34.99"), new BigDecimal("44.99"), 250,
                        "https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=800&h=800&fit=crop"),
                        "Triple-wall vacuum insulation, keeps cold 24h / hot 12h.",
                        List.of("home", "water-bottle", "insulated", "eco", "hydration")),

                product("Handmade Ceramic Bowl Set", "HOME-CB-013",
                        "Home & Kitchen", "ArtisanHome",
                        new BigDecimal("44.99"), new BigDecimal("59.99"), 70,
                        "https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=800&h=800&fit=crop"),
                        "Set of 4 hand-thrown stoneware bowls, microwave and dishwasher safe.",
                        List.of("home", "ceramic", "bowls", "handmade", "kitchen")),

                product("Smart Air Purifier HEPA", "HOME-AP-014",
                        "Home & Kitchen", "PureAir",
                        new BigDecimal("249.99"), new BigDecimal("329.99"), 55,
                        "https://images.unsplash.com/photo-1585771724684-38269d6639fd?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1585771724684-38269d6639fd?w=800&h=800&fit=crop"),
                        "True HEPA H13 filter, app-controlled, covers up to 500 sq ft.",
                        List.of("home", "air-purifier", "hepa", "smart", "iot")),

                // -- Sports -----------------------------------------------
                product("Yoga Mat Premium 6mm", "SPRT-YM-015",
                        "Sports", "FitGear",
                        new BigDecimal("39.99"), new BigDecimal("49.99"), 200,
                        "https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=800&h=800&fit=crop"),
                        "Non-slip TPE material, alignment lines, and carrying strap included.",
                        List.of("sports", "yoga", "fitness", "mat", "exercise")),

                product("Adjustable Dumbbell Set 50lb", "SPRT-DB-016",
                        "Sports", "PowerFit",
                        new BigDecimal("349.99"), new BigDecimal("449.99"), 40,
                        "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800&h=800&fit=crop"),
                        "Quick-change weight system from 5-50 lbs, anti-slip grip.",
                        List.of("sports", "dumbbells", "fitness", "weights", "home-gym")),

                product("Running Shoes AirMax", "SPRT-RS-017",
                        "Sports", "NikeRun",
                        new BigDecimal("129.99"), new BigDecimal("159.99"), 85,
                        "https://images.unsplash.com/photo-1460353581641-37baddab0fa2?w=800&h=800&fit=crop",
                        List.of(
                                "https://images.unsplash.com/photo-1460353581641-37baddab0fa2?w=800&h=800&fit=crop",
                                "https://images.unsplash.com/photo-1491553895911-0055eca6402d?w=800&h=800&fit=crop"),
                        "Lightweight mesh upper, responsive cushioning, and carbon rubber outsole.",
                        List.of("sports", "running", "shoes", "athletic", "performance")),

                product("Resistance Band Set Pro", "SPRT-RB-018",
                        "Sports", "FlexFit",
                        new BigDecimal("29.99"), new BigDecimal("39.99"), 300,
                        "https://images.unsplash.com/photo-1598289431512-b97b0917affc?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1598289431512-b97b0917affc?w=800&h=800&fit=crop"),
                        "5 resistance levels, door anchor, ankle straps, and carry bag.",
                        List.of("sports", "resistance-bands", "fitness", "home-workout", "portable")),

                // -- Additional Electronics & Home (to reach 22 products) --
                product("DSLR Camera Body 24MP", "ELEC-CM-019",
                        "Electronics", "CanonPro",
                        new BigDecimal("899.99"), new BigDecimal("1099.99"), 25,
                        "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&h=800&fit=crop",
                        List.of(
                                "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&h=800&fit=crop",
                                "https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=800&h=800&fit=crop"),
                        "24.2MP APS-C sensor, 4K video, dual pixel autofocus, weather-sealed body.",
                        List.of("electronics", "camera", "dslr", "photography", "4k")),

                product("Noise Cancelling Earbuds", "ELEC-EB-020",
                        "Electronics", "SoundTech",
                        new BigDecimal("149.99"), new BigDecimal("199.99"), 110,
                        "https://images.unsplash.com/photo-1590658268037-6bf12f032f55?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1590658268037-6bf12f032f55?w=800&h=800&fit=crop"),
                        "Active noise cancelling, transparency mode, 8-hour battery per charge.",
                        List.of("electronics", "earbuds", "anc", "wireless", "audio")),

                product("Smart Home Hub", "ELEC-HB-021",
                        "Electronics", "SmartLife",
                        new BigDecimal("99.99"), new BigDecimal("129.99"), 65,
                        "https://images.unsplash.com/photo-1558089687-f282d8132f71?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1558089687-f282d8132f71?w=800&h=800&fit=crop"),
                        "Central control for all smart devices, voice assistant, and 7-inch display.",
                        List.of("electronics", "smart-home", "hub", "iot", "voice")),

                product("Ergonomic Office Chair", "HOME-OC-022",
                        "Home & Kitchen", "ComfortSeating",
                        new BigDecimal("449.99"), new BigDecimal("599.99"), 35,
                        "https://images.unsplash.com/photo-1580480055273-228ff5388ef8?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1580480055273-228ff5388ef8?w=800&h=800&fit=crop"),
                        "Adjustable lumbar support, breathable mesh, and 4D armrests.",
                        List.of("home", "furniture", "office", "chair", "ergonomic"))
        );
    }

    // =====================================================================
    // WAREHOUSE BUILDERS
    // =====================================================================

    private WarehouseDTO createMainWarehouse(InventoryService inventoryService) {
        return inventoryService.createWarehouse(WarehouseRequestDTO.builder()
                .name("Main Warehouse")
                .code("MAIN-001")
                .address("123 Commerce Street, Industrial District")
                .city("New York")
                .state("NY")
                .country("USA")
                .postalCode("10001")
                .managerName("John Smith")
                .managerEmail("john.smith@commercepro.com")
                .managerPhone("+1-555-0100")
                .isActive(true)
                .isDefault(true)
                .build());
    }

    private WarehouseDTO createWestWarehouse(InventoryService inventoryService) {
        return inventoryService.createWarehouse(WarehouseRequestDTO.builder()
                .name("West Coast Warehouse")
                .code("WEST-002")
                .address("456 Logistics Blvd, Port Area")
                .city("Los Angeles")
                .state("CA")
                .country("USA")
                .postalCode("90001")
                .managerName("Sarah Johnson")
                .managerEmail("sarah.johnson@commercepro.com")
                .managerPhone("+1-555-0200")
                .isActive(true)
                .isDefault(false)
                .build());
    }

    // =====================================================================
    // INVENTORY BUILDER
    // =====================================================================

    private InventoryRequestDTO buildInventoryRequest(
            String productId, String warehouseId,
            int quantity, int reserved,
            int baseStock, BigDecimal unitCost,
            String zone, String aisle, int index) {

        return InventoryRequestDTO.builder()
                .productId(productId)
                .warehouseId(warehouseId)
                .quantity(quantity)
                .reserved(reserved)
                .lowStockThreshold(Math.max(5,  (int) (baseStock * 0.15)))
                .reorderPoint(Math.max(10,       (int) (baseStock * 0.25)))
                .reorderQuantity(                (int) (baseStock * 0.50))
                .unitCost(unitCost)
                .binLocation(zone + "-" + aisle + "-" + String.format("%03d", index + 1))
                .zone(zone)
                .aisle(aisle)
                .trackInventory(true)
                .build();
    }

    // =====================================================================
    // ORDER BUILDER HELPERS
    // =====================================================================

    private List<OrderItemRequestDTO> buildItems(
            List<ProductSummaryDTO> products, int fromIndex, int count) {

        List<OrderItemRequestDTO> items = new ArrayList<>();
        int end = Math.min(fromIndex + count, products.size());
        for (int i = fromIndex; i < end; i++) {
            items.add(OrderItemRequestDTO.builder()
                    .productId(products.get(i).getId())
                    .quantity(i % 2 == 0 ? 1 : 2)
                    .taxRate(new BigDecimal("0.08"))
                    .itemDiscount(BigDecimal.ZERO)
                    .build());
        }
        return items;
    }

    // =====================================================================
    // PRODUCT BUILDER HELPERS
    // =====================================================================

    private ProductRequestDTO product(
            String name, String sku, String category, String brand,
            BigDecimal price, BigDecimal comparePrice, int stock,
            String image, List<String> gallery, String description, List<String> tags) {

        return productWithVariants(name, sku, category, brand,
                price, comparePrice, stock, image, gallery, description, tags,
                null, null);
    }

    private ProductRequestDTO productWithVariants(
            String name, String sku, String category, String brand,
            BigDecimal price, BigDecimal comparePrice, int stock,
            String image, List<String> gallery, String description, List<String> tags,
            List<ProductAttributeDTO> attributes, List<ProductVariantDTO> variants) {

        return ProductRequestDTO.builder()
                .name(name)
                .sku(sku)
                .description(description)
                .shortDescription(description.substring(0, Math.min(100, description.length())) + "...")
                .category(category)
                .brand(brand)
                .price(price)
                .compareAtPrice(comparePrice)
                .cost(price.multiply(new BigDecimal("0.60")))
                .stock(stock)
                .lowStockThreshold(10)
                .status("active")
                .visibility("visible")
                .image(image)
                .gallery(gallery)
                .tags(tags)
                .featured(Math.random() > 0.70)
                .trackInventory(true)
                .allowBackorders(false)
                .vendor(brand)
                .productType("Physical")
                .attributes(attributes != null ? attributes : new ArrayList<>())
                .variants(variants != null ? variants : new ArrayList<>())
                .build();
    }

    // =====================================================================
    // UTILITY HELPERS
    // =====================================================================

    private List<ProductSummaryDTO> slice(List<ProductSummaryDTO> list, int from, int count, int total) {
        int safeFrom = Math.min(from, total - 1);
        int safeTo   = Math.min(safeFrom + count, total);
        return list.subList(safeFrom, safeTo);
    }

    private int proportional(int base, double minFraction, double maxFraction) {
        double fraction = minFraction + Math.random() * (maxFraction - minFraction);
        return Math.max(1, (int) (base * fraction));
    }

    private BigDecimal unitCost(BigDecimal price) {
        return price.multiply(new BigDecimal("0.60"));
    }
}
