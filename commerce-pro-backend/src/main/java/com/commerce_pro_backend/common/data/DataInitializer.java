package com.commerce_pro_backend.common.data;

import com.commerce_pro_backend.catalog.product.dto.ProductAttributeDTO;
import com.commerce_pro_backend.catalog.product.dto.ProductRequestDTO;
import com.commerce_pro_backend.catalog.product.dto.ProductSummaryDTO;
import com.commerce_pro_backend.catalog.product.dto.ProductVariantDTO;
import com.commerce_pro_backend.catalog.product.service.ProductService;
import com.commerce_pro_backend.inventory.dto.InventoryRequestDTO;
import com.commerce_pro_backend.inventory.dto.WarehouseDTO;
import com.commerce_pro_backend.inventory.dto.WarehouseRequestDTO;
import com.commerce_pro_backend.inventory.service.InventoryService;
import com.commerce_pro_backend.order.dto.CreateOrderRequestDTO;
import com.commerce_pro_backend.order.dto.OrderAddressDTO;
import com.commerce_pro_backend.order.dto.OrderCancelRequest;
import com.commerce_pro_backend.order.dto.OrderHoldRequest;
import com.commerce_pro_backend.order.dto.OrderItemRequestDTO;
import com.commerce_pro_backend.order.dto.TrackingUpdateRequest;
import com.commerce_pro_backend.order.enums.OrderSource;
import com.commerce_pro_backend.order.service.OrderService;
import com.commerce_pro_backend.user_identity.repository.UserRepository;
import com.commerce_pro_backend.customer.dto.CustomerRequestDTO;
import com.commerce_pro_backend.customer.dto.GroupDTO;
import com.commerce_pro_backend.customer.service.CustomerGroupService;
import com.commerce_pro_backend.customer.service.CustomerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Development-only data initialiser.
 *
 * Execution order (controlled via Spring @Order):
 *   1. Products  — catalogue must exist before anything else can reference it
 *   2. Inventory — warehouses + stock records per product
 *   3. Orders    — real order lifecycle scenarios using the seeded products
 *
 * DDL is create-drop so the schema is always fresh on restart.
 * Every section is fully isolated in try/catch so a failure in one
 * module never blocks the rest of the seed data from loading.
 *
 * ── WHY THE SECURITY CONTEXT FIX IS NEEDED ───────────────────────────────────
 * OrderService.createOrder() (and every other mutating method) calls
 * CurrentUserService.getCurrentUserId() on its very first line.
 * CurrentUserService reads from SecurityContextHolder, which is EMPTY during
 * CommandLineRunner startup — there is no HTTP request and no JWT token.
 * Without an authenticated principal, CurrentUserService throws
 * ApiException.unauthorized("Unauthenticated request"), which causes every
 * single createOrder / confirmOrder / markShipped call to fail silently.
 *
 * The fix: call authenticateAsSuperAdmin() before the first OrderService call,
 * and clearSecurityContext() after the seed block finishes. This installs a
 * synthetic UsernamePasswordAuthenticationToken for the "superadmin" user into
 * the SecurityContextHolder so getCurrentUserId() can resolve a real User ID
 * from the database. The superadmin user is guaranteed to exist because
 * SuperAdminSetupService runs via @PostConstruct before any CommandLineRunner.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. PRODUCTS
    // ─────────────────────────────────────────────────────────────────────────

    @Bean
    @Profile("dev")
    @Order(1)
    CommandLineRunner seedProducts(ProductService productService) {
        return args -> {
            log.info("━━━ [Seed] Products ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // Clear any stale data from a previous run
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

    // ─────────────────────────────────────────────────────────────────────────
    // 2. INVENTORY  (warehouses + stock)
    // ─────────────────────────────────────────────────────────────────────────

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
                    // Main warehouse — 70–100 % of declared stock
                    int mainQty      = proportional(baseStock, 0.70, 1.00);
                    int mainReserved = proportional(mainQty,   0.05, 0.15);
                    inventoryService.createInventory(
                            buildInventoryRequest(
                                    product.getId(), mainWarehouse.getId(),
                                    mainQty, mainReserved, baseStock, unitCost,
                                    zones[i % 4], aisles[i % 5], i));

                    // West warehouse — every third product, 20–30 % of stock
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

    // ─────────────────────────────────────────────────────────────────────────
    // 3. ORDERS  (diverse status scenarios)
    //
    // FIX: authenticateAsSuperAdmin() is called before the first OrderService
    // call so that CurrentUserService.getCurrentUserId() can resolve a real
    // User ID from the SecurityContextHolder.  The context is cleared with
    // clearSecurityContext() in the finally block when seeding is done.
    // ─────────────────────────────────────────────────────────────────────────

    @Bean
    @Profile("dev")
    @Order(3)
    CommandLineRunner seedOrders(ProductService productService,
                                 OrderService orderService,
                                 UserDetailsService userDetailsService,
                                 UserRepository userRepository) {
        return args -> {
            log.info("━━━ [Seed] Orders ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            List<ProductSummaryDTO> products = productService.getAllProducts();
            if (products.isEmpty()) {
                log.warn("[Seed] No products found — skipping order seed");
                return;
            }

            // ── SECURITY CONTEXT SETUP ────────────────────────────────────────
            // SuperAdminSetupService runs @PostConstruct (before CommandLineRunner)
            // so "superadmin" is guaranteed to exist in the users table by now.
            // We load its UserDetails and install them into the SecurityContextHolder
            // so that CurrentUserService.getCurrentUserId() resolves a valid User ID.
            try {
                authenticateAsSuperAdmin(userDetailsService);
                log.info("[Seed] SecurityContext set to superadmin — order seeding can proceed");
            } catch (Exception e) {
                log.error("[Seed] Could not set SecurityContext: {}. Order seeding aborted.", e.getMessage());
                return;
            }

            // ── SEED ORDERS ───────────────────────────────────────────────────
            try {
                int total = products.size();

                seedOrderDraft         (orderService, slice(products, 0,  2, total));
                seedOrderPendingPayment(orderService, slice(products, 1,  2, total));
                seedOrderConfirmed     (orderService, slice(products, 2,  3, total));
                seedOrderOnHold        (orderService, slice(products, 3,  2, total));
                seedOrderProcessing    (orderService, slice(products, 4,  3, total));
                seedOrderShipped       (orderService, slice(products, 5,  2, total));
                seedOrderDelivered     (orderService, slice(products, 6,  2, total));
                seedOrderDeliveredClose(orderService, slice(products, 7,  3, total));
                seedOrderCancelled     (orderService, slice(products, 8,  1, total));
                seedOrderHighValue     (orderService, slice(products, 9,  4, total));

                log.info("[Seed] Orders complete — 10 scenario orders created");

            } finally {
                // Always clear the synthetic security context when seeding is done
                // so it does not persist into subsequent application code.
                clearSecurityContext();
                log.info("[Seed] SecurityContext cleared after order seeding");
            }
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. FULFILLMENT  (carriers, pick lists, shipments)
    // ─────────────────────────────────────────────────────────────────────────

    @Bean
    @Profile("dev")
    @Order(4)
    CommandLineRunner seedFulfillment(
            com.commerce_pro_backend.fulfillment.service.FulfillmentService fulfillmentService,
            com.commerce_pro_backend.fulfillment.service.ShipmentService shipmentService,
            com.commerce_pro_backend.order.repository.OrderRepository orderRepository,
            UserDetailsService userDetailsService) {
        return args -> {
            log.info("━━━ [Seed] Fulfillment ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            try {
                authenticateAsSuperAdmin(userDetailsService);

                // ── Carriers ──────────────────────────────────────────────────
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

                // ── Shipping Rules ─────────────────────────────────────────────
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
                expressRule.setConditionMin(new java.math.BigDecimal("100.00"));
                expressRule.setShippingMethod("Express");
                expressRule.setServiceLevel("1-2 Business Days");
                expressRule.setPriority(50);
                expressRule.setIsActive(true);
                fulfillmentService.createRule(expressRule);

                log.info("[Seed] Created 2 shipping rules");

                // ── Pick List — from CONFIRMED orders ─────────────────────────
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

                // ── Shipment — from SHIPPED orders ────────────────────────────
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

                    // Add a tracking event — IN_TRANSIT
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

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Customer
    // ─────────────────────────────────────────────────────────────────────────

    @Bean
    @Profile("dev")
    @Order(5)
    CommandLineRunner seedCustomers(
            CustomerService customerService,
            CustomerGroupService groupService,
            UserDetailsService userDetailsService) {
        return args -> {
            log.info("━━━ [Seed] Customers ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            try {
                // Authenticate as superadmin so CurrentUserService resolves properly
                authenticateAsSuperAdmin(userDetailsService);

                // Groups
                GroupDTO.Response vipGroup = groupService.createGroup(GroupDTO.Request.builder()
                        .name("VIP Buyers").description("High-value repeat customers")
                        .colorHex("#F59E0B").isActive(true).build());

                GroupDTO.Response wholesaleGroup = groupService.createGroup(GroupDTO.Request.builder()
                        .name("Wholesale Accounts").description("Trade and bulk purchasers")
                        .colorHex("#0D9488").isActive(true).build());

                log.info("[Seed] Created 2 customer groups");

                // Customers
                String[][] customers = {
                    {"Alice",  "Johnson",  "alice.johnson@example.com",  "+1-212-555-0101", "STOREFRONT"},
                    {"Bob",    "Smith",    "bob.smith@example.com",      "+1-310-555-0102", "STOREFRONT"},
                    {"Carol",  "Williams", "carol.williams@example.com", "+1-312-555-0103", "REFERRAL"},
                    {"David",  "Brown",    "david.brown@example.com",    "+1-415-555-0104", "STOREFRONT"},
                    {"Eve",    "Davis",    "eve.davis@example.com",      "+1-206-555-0105", "SOCIAL"},
                    {"Frank",  "Miller",   "frank.miller@example.com",   "+1-713-555-0106", "STOREFRONT"},
                    {"Grace",  "Lee",      "grace.lee@example.com",      "+1-602-555-0107", "EMAIL"},
                    {"Henry",  "Wilson",   "henry.wilson@example.com",   "+1-305-555-0108", "STOREFRONT"},
                    {"Irene",  "Taylor",   "irene.taylor@example.com",   "+1-617-555-0109", "REFERRAL"},
                    {"James",  "Anderson", "james.anderson@example.com", "+1-646-555-0110", "STOREFRONT"},
                };

                int created = 0;
                for (String[] c : customers) {
                    try {
                        customerService.createCustomer(CustomerRequestDTO.builder()
                                .firstName(c[0]).lastName(c[1]).email(c[2]).phone(c[3])
                                .acquisitionSource(c[4]).marketingOptIn(true)
                                .preferredCurrency("USD").preferredLanguage("en")
                                .build());
                        created++;
                    } catch (Exception e) {
                        log.warn("[Seed] Customer seed skipped for {}: {}", c[2], e.getMessage());
                    }
                }
                log.info("[Seed] Customers complete — {} created", created);

            } catch (Exception e) {
                log.error("[Seed] Customer seed failed: {}", e.getMessage(), e);
            } finally {
                clearSecurityContext();
            }
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECURITY CONTEXT HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Loads the "superadmin" user via UserDetailsService (which hits the DB) and
     * installs a UsernamePasswordAuthenticationToken into the SecurityContextHolder.
     *
     * This is intentionally the same mechanism used by JwtAuthenticationFilter
     * so CurrentUserService sees a properly structured Authentication object.
     *
     * SuperAdminSetupService creates the superadmin user via @PostConstruct,
     * which runs before any CommandLineRunner, so the user always exists here.
     */
    private void authenticateAsSuperAdmin(UserDetailsService userDetailsService) {
        var userDetails = userDetailsService.loadUserByUsername("superadmin");
        var auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * Clears the SecurityContextHolder after seed operations finish.
     * Must always be called in a finally block to prevent the synthetic
     * context from leaking into application request handling.
     */
    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    

    // ═════════════════════════════════════════════════════════════════════════
    // PRODUCT CATALOGUE
    // ═════════════════════════════════════════════════════════════════════════

    private List<ProductRequestDTO> buildProductCatalogue() {
        return List.of(

                // ── Electronics ───────────────────────────────────────────────
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

                // ── Fashion ───────────────────────────────────────────────────
                product("Classic Leather Backpack", "FASH-BP-006",
                        "Fashion", "Heritage Leather",
                        new BigDecimal("149.99"), new BigDecimal("199.99"), 85,
                        "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&h=800&fit=crop",
                        List.of(
                                "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&h=800&fit=crop",
                                "https://images.unsplash.com/photo-1622560480605-d83c853bc5c3?w=800&h=800&fit=crop"),
                        "Handcrafted genuine leather backpack with laptop compartment and multiple pockets.",
                        List.of("fashion", "bags", "leather", "backpack", "accessories")),

                product("Premium Sunglasses UV400", "FASH-SG-007",
                        "Fashion", "RayStyle",
                        new BigDecimal("89.99"), new BigDecimal("129.99"), 120,
                        "https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=800&h=800&fit=crop",
                        List.of(
                                "https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=800&h=800&fit=crop",
                                "https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=800&h=800&fit=crop"),
                        "Polarized lenses with 100% UV protection in a timeless aviator design.",
                        List.of("fashion", "sunglasses", "accessories", "uv-protection", "premium")),

                product("Minimalist Leather Wallet", "FASH-WL-008",
                        "Fashion", "LeatherCraft",
                        new BigDecimal("49.99"), new BigDecimal("69.99"), 250,
                        "https://images.unsplash.com/photo-1627123424574-724758594e93?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1627123424574-724758594e93?w=800&h=800&fit=crop"),
                        "Slim bifold design with RFID blocking technology and premium full-grain leather.",
                        List.of("fashion", "wallet", "leather", "accessories", "minimalist")),

                productWithVariants(
                        "Canvas Sneakers Classic", "FASH-SN-009",
                        "Fashion", "StreetWear",
                        new BigDecimal("59.99"), new BigDecimal("79.99"), 300,
                        "https://images.unsplash.com/photo-1600269452121-4f2416e55c28?w=800&h=800&fit=crop",
                        List.of(
                                "https://images.unsplash.com/photo-1600269452121-4f2416e55c28?w=800&h=800&fit=crop",
                                "https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?w=800&h=800&fit=crop"),
                        "Iconic canvas sneakers with vulcanized rubber sole and cushioned insole.",
                        List.of("fashion", "shoes", "sneakers", "casual", "canvas"),
                        List.of(
                                ProductAttributeDTO.builder().name("Size")
                                        .values(List.of("7", "8", "9", "10", "11")).displayOrder(1).build(),
                                ProductAttributeDTO.builder().name("Color")
                                        .values(List.of("White", "Black", "Red", "Navy")).displayOrder(2).build()),
                        List.of(
                                ProductVariantDTO.builder().name("Size")
                                        .options(List.of("7", "8", "9", "10", "11")).build(),
                                ProductVariantDTO.builder().name("Color")
                                        .options(List.of("White", "Black", "Red", "Navy")).build())),

                product("Designer Watch Automatic", "FASH-WT-010",
                        "Fashion", "TimePiece",
                        new BigDecimal("299.99"), new BigDecimal("399.99"), 45,
                        "https://images.unsplash.com/photo-1524592094714-0f0654e20314?w=800&h=800&fit=crop",
                        List.of(
                                "https://images.unsplash.com/photo-1524592094714-0f0654e20314?w=800&h=800&fit=crop",
                                "https://images.unsplash.com/photo-1522312346375-d1a52e2b99b3?w=800&h=800&fit=crop"),
                        "Self-winding automatic movement with sapphire crystal and stainless steel case.",
                        List.of("fashion", "watch", "luxury", "automatic", "accessories")),

                // ── Home & Kitchen ────────────────────────────────────────────
                product("Pour-Over Coffee Maker Set", "HOME-CM-011",
                        "Home & Kitchen", "BrewMaster",
                        new BigDecimal("59.99"), new BigDecimal("79.99"), 95,
                        "https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=800&h=800&fit=crop",
                        List.of(
                                "https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=800&h=800&fit=crop",
                                "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=800&h=800&fit=crop"),
                        "Complete pour-over set with glass carafe, ceramic dripper, and filter papers.",
                        List.of("home", "kitchen", "coffee", "brewing", "pour-over")),

                product("Stainless Steel Water Bottle", "HOME-WB-012",
                        "Home & Kitchen", "HydroLife",
                        new BigDecimal("34.99"), new BigDecimal("44.99"), 400,
                        "https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=800&h=800&fit=crop"),
                        "Double-wall vacuum insulated bottle keeps drinks cold for 24 hours or hot for 12.",
                        List.of("home", "kitchen", "water-bottle", "eco-friendly", "insulated")),

                product("Ceramic Dinnerware Set", "HOME-DW-013",
                        "Home & Kitchen", "ArtisanHome",
                        new BigDecimal("129.99"), new BigDecimal("169.99"), 60,
                        "https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=800&h=800&fit=crop",
                        List.of(
                                "https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=800&h=800&fit=crop",
                                "https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=800&h=800&fit=crop"),
                        "Handcrafted ceramic dinnerware set for 4 with minimalist modern design.",
                        List.of("home", "kitchen", "dinnerware", "ceramic", "dining")),

                product("Smart Air Purifier", "HOME-AP-014",
                        "Home & Kitchen", "PureAir",
                        new BigDecimal("199.99"), new BigDecimal("249.99"), 40,
                        "https://images.unsplash.com/photo-1585771724684-38269d6639fd?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1585771724684-38269d6639fd?w=800&h=800&fit=crop"),
                        "HEPA filtration with app control, air quality monitoring, and quiet operation.",
                        List.of("home", "appliances", "air-purifier", "smart-home", "health")),

                // ── Sports ────────────────────────────────────────────────────
                product("Yoga Mat Premium Non-Slip", "SPRT-YM-015",
                        "Sports", "FitGear",
                        new BigDecimal("45.99"), new BigDecimal("59.99"), 150,
                        "https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=800&h=800&fit=crop",
                        List.of(
                                "https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=800&h=800&fit=crop",
                                "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=800&h=800&fit=crop"),
                        "Eco-friendly TPE material with alignment lines and carrying strap.",
                        List.of("sports", "fitness", "yoga", "mat", "exercise")),

                product("Adjustable Dumbbells Set", "SPRT-DB-016",
                        "Sports", "PowerFit",
                        new BigDecimal("299.99"), new BigDecimal("399.99"), 35,
                        "https://images.unsplash.com/photo-1638536532686-d610adfc8e5c?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1638536532686-d610adfc8e5c?w=800&h=800&fit=crop"),
                        "Adjustable from 5 to 52.5 lbs with easy dial system. Space-saving design.",
                        List.of("sports", "fitness", "dumbbells", "strength", "home-gym")),

                productWithVariants(
                        "Running Shoes Performance", "SPRT-RS-017",
                        "Sports", "NikeRun",
                        new BigDecimal("129.99"), new BigDecimal("159.99"), 200,
                        "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&h=800&fit=crop",
                        List.of(
                                "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&h=800&fit=crop",
                                "https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?w=800&h=800&fit=crop"),
                        "Responsive cushioning and breathable mesh upper for maximum comfort.",
                        List.of("sports", "running", "shoes", "athletic", "performance"),
                        List.of(
                                ProductAttributeDTO.builder().name("Size")
                                        .values(List.of("7", "8", "9", "10", "11", "12")).displayOrder(1).build(),
                                ProductAttributeDTO.builder().name("Color")
                                        .values(List.of("Black/Red", "White/Blue", "Grey/Neon")).displayOrder(2).build()),
                        List.of(
                                ProductVariantDTO.builder().name("Size")
                                        .options(List.of("7", "8", "9", "10", "11", "12")).build(),
                                ProductVariantDTO.builder().name("Color")
                                        .options(List.of("Black/Red", "White/Blue", "Grey/Neon")).build())),

                product("Resistance Bands Set", "SPRT-RB-018",
                        "Sports", "FlexFit",
                        new BigDecimal("24.99"), new BigDecimal("34.99"), 500,
                        "https://images.unsplash.com/photo-1598289431512-b97b0917affc?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1598289431512-b97b0917affc?w=800&h=800&fit=crop"),
                        "5 levels of resistance with handles, ankle straps, and door anchor.",
                        List.of("sports", "fitness", "resistance-bands", "training", "home-workout")),

                // ── Electronics (continued) ───────────────────────────────────
                product("Professional Camera DSLR", "ELEC-CM-019",
                        "Electronics", "CanonPro",
                        new BigDecimal("899.99"), new BigDecimal("1099.99"), 25,
                        "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&h=800&fit=crop",
                        List.of(
                                "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&h=800&fit=crop",
                                "https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=800&h=800&fit=crop"),
                        "24.1MP full-frame sensor, 4K video, and advanced autofocus system.",
                        List.of("electronics", "camera", "photography", "dslr", "professional")),

                product("Wireless Earbuds Pro", "ELEC-EB-020",
                        "Electronics", "SoundTech",
                        new BigDecimal("149.99"), new BigDecimal("199.99"), 300,
                        "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=800&h=800&fit=crop",
                        List.of(
                                "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=800&h=800&fit=crop",
                                "https://images.unsplash.com/photo-1606220588913-b3aacb4d2f46?w=800&h=800&fit=crop"),
                        "Active noise cancellation, transparency mode, and 24-hour battery with case.",
                        List.of("electronics", "audio", "earbuds", "wireless", "noise-cancelling")),

                product("Smart Home Hub", "ELEC-SH-021",
                        "Electronics", "SmartLife",
                        new BigDecimal("79.99"), new BigDecimal("99.99"), 80,
                        "https://images.unsplash.com/photo-1558089687-f282ffcbc126?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1558089687-f282ffcbc126?w=800&h=800&fit=crop"),
                        "Control all your smart devices with voice commands and automation.",
                        List.of("electronics", "smart-home", "hub", "automation", "iot")),

                product("Ergonomic Office Chair", "HOME-OC-022",
                        "Home & Kitchen", "ComfortSeating",
                        new BigDecimal("349.99"), new BigDecimal("449.99"), 30,
                        "https://images.unsplash.com/photo-1505843490538-5133c6c7d0e1?w=800&h=800&fit=crop",
                        List.of("https://images.unsplash.com/photo-1505843490538-5133c6c7d0e1?w=800&h=800&fit=crop"),
                        "Adjustable lumbar support, breathable mesh, and 4D armrests.",
                        List.of("home", "furniture", "office", "chair", "ergonomic"))
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ORDER SCENARIO HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    /** Scenario 1 — DRAFT: created, not yet confirmed */
    private void seedOrderDraft(OrderService orderService, List<ProductSummaryDTO> products) {
        try {
            orderService.createOrder(buildOrderRequest(
                    "Alice Johnson", "alice.johnson@example.com", "+1-212-555-0101",
                    OrderSource.STOREFRONT,
                    "10 West 34th Street", "New York", "NY", "10001", "USA",
                    new BigDecimal("5.99"), BigDecimal.ZERO, "Standard Shipping", null,
                    buildItems(products, 0, 2)));
            log.info("[Seed] DRAFT order created — Alice Johnson");
        } catch (Exception e) {
            log.error("[Seed] DRAFT order failed: {}", e.getMessage());
        }
    }

    /** Scenario 2 — PENDING_PAYMENT: awaiting payment capture */
    private void seedOrderPendingPayment(OrderService orderService, List<ProductSummaryDTO> products) {
        try {
            orderService.createOrder(buildOrderRequest(
                    "Bob Smith", "bob.smith@example.com", "+1-310-555-0102",
                    OrderSource.STOREFRONT,
                    "456 Sunset Blvd", "Los Angeles", "CA", "90028", "USA",
                    new BigDecimal("9.99"), BigDecimal.ZERO, "Express Shipping", null,
                    buildItems(products, 0, 1)));
            log.info("[Seed] PENDING_PAYMENT order created — Bob Smith");
        } catch (Exception e) {
            log.error("[Seed] PENDING_PAYMENT order failed: {}", e.getMessage());
        }
    }

    /** Scenario 3 — CONFIRMED: payment captured, inventory reserved */
    private void seedOrderConfirmed(OrderService orderService, List<ProductSummaryDTO> products) {
        try {
            var response = orderService.createOrder(buildOrderRequest(
                    "Carol Williams", "carol.williams@example.com", "+1-312-555-0103",
                    OrderSource.MANUAL,
                    "789 Michigan Ave", "Chicago", "IL", "60611", "USA",
                    new BigDecimal("12.99"), new BigDecimal("10.00"), "Priority Shipping", "SAVE10",
                    buildItems(products, 0, 3)));
            orderService.confirmOrder(response.getId());
            log.info("[Seed] CONFIRMED order created — Carol Williams");
        } catch (Exception e) {
            log.error("[Seed] CONFIRMED order failed: {}", e.getMessage());
        }
    }

    /** Scenario 4 — ON_HOLD: confirmed then held for billing verification */
    private void seedOrderOnHold(OrderService orderService, List<ProductSummaryDTO> products) {
        try {
            var response = orderService.createOrder(buildOrderRequest(
                    "David Brown", "david.brown@example.com", "+1-415-555-0104",
                    OrderSource.API,
                    "321 Market Street", "San Francisco", "CA", "94105", "USA",
                    new BigDecimal("8.99"), BigDecimal.ZERO, "Standard Shipping", null,
                    buildItems(products, 0, 2)));
            orderService.confirmOrder(response.getId());
            OrderHoldRequest holdReq = new OrderHoldRequest();
            holdReq.setReason("Billing address verification required");
            orderService.holdOrder(response.getId(), holdReq);
            log.info("[Seed] ON_HOLD order created — David Brown");
        } catch (Exception e) {
            log.error("[Seed] ON_HOLD order failed: {}", e.getMessage());
        }
    }

    /** Scenario 5 — PROCESSING: confirmed and moved to warehouse queue */
    private void seedOrderProcessing(OrderService orderService, List<ProductSummaryDTO> products) {
        try {
            var response = orderService.createOrder(buildOrderRequest(
                    "Eve Davis", "eve.davis@example.com", "+1-206-555-0105",
                    OrderSource.STOREFRONT,
                    "654 Pike Place", "Seattle", "WA", "98101", "USA",
                    new BigDecimal("7.99"), BigDecimal.ZERO, "Standard Shipping", null,
                    buildItems(products, 0, 3)));
            orderService.confirmOrder(response.getId());
            orderService.markProcessing(response.getId());
            log.info("[Seed] PROCESSING order created — Eve Davis");
        } catch (Exception e) {
            log.error("[Seed] PROCESSING order failed: {}", e.getMessage());
        }
    }

    /** Scenario 6 — SHIPPED: dispatched with a real tracking number */
    private void seedOrderShipped(OrderService orderService, List<ProductSummaryDTO> products) {
        try {
            var response = orderService.createOrder(buildOrderRequest(
                    "Frank Miller", "frank.miller@example.com", "+1-713-555-0106",
                    OrderSource.STOREFRONT,
                    "987 Main Street", "Houston", "TX", "77002", "USA",
                    new BigDecimal("11.99"), BigDecimal.ZERO, "Express Shipping", null,
                    buildItems(products, 0, 2)));
            orderService.confirmOrder(response.getId());
            orderService.markProcessing(response.getId());
            TrackingUpdateRequest trackingReq = new TrackingUpdateRequest();
            trackingReq.setTrackingNumber("UPS1234567890");
            trackingReq.setCarrier("UPS");
            orderService.markShipped(response.getId(), trackingReq);
            log.info("[Seed] SHIPPED order created — Frank Miller (UPS1234567890)");
        } catch (Exception e) {
            log.error("[Seed] SHIPPED order failed: {}", e.getMessage());
        }
    }

    /** Scenario 7 — DELIVERED: successfully received by customer */
    private void seedOrderDelivered(OrderService orderService, List<ProductSummaryDTO> products) {
        try {
            var response = orderService.createOrder(buildOrderRequest(
                    "Grace Lee", "grace.lee@example.com", "+1-602-555-0107",
                    OrderSource.STOREFRONT,
                    "246 Central Ave", "Phoenix", "AZ", "85004", "USA",
                    new BigDecimal("6.99"), BigDecimal.ZERO, "Standard Shipping", null,
                    buildItems(products, 0, 2)));
            orderService.confirmOrder(response.getId());
            orderService.markProcessing(response.getId());
            TrackingUpdateRequest trackingReq = new TrackingUpdateRequest();
            trackingReq.setTrackingNumber("FEDEX9988776655");
            trackingReq.setCarrier("FedEx");
            orderService.markShipped(response.getId(), trackingReq);
            orderService.markDelivered(response.getId());
            log.info("[Seed] DELIVERED order created — Grace Lee");
        } catch (Exception e) {
            log.error("[Seed] DELIVERED order failed: {}", e.getMessage());
        }
    }

    /** Scenario 8 — CLOSED: fully delivered and settled */
    private void seedOrderDeliveredClose(OrderService orderService, List<ProductSummaryDTO> products) {
        try {
            var response = orderService.createOrder(buildOrderRequest(
                    "Henry Wilson", "henry.wilson@example.com", "+1-305-555-0108",
                    OrderSource.MANUAL,
                    "135 Biscayne Blvd", "Miami", "FL", "33132", "USA",
                    new BigDecimal("14.99"), new BigDecimal("15.00"), "Priority Shipping", "WELCOME15",
                    buildItems(products, 0, 3)));
            orderService.confirmOrder(response.getId());
            orderService.markProcessing(response.getId());
            TrackingUpdateRequest trackingReq = new TrackingUpdateRequest();
            trackingReq.setTrackingNumber("DHL4455667788");
            trackingReq.setCarrier("DHL");
            orderService.markShipped(response.getId(), trackingReq);
            orderService.markDelivered(response.getId());
            orderService.closeOrder(response.getId());
            log.info("[Seed] CLOSED order created — Henry Wilson");
        } catch (Exception e) {
            log.error("[Seed] CLOSED order failed: {}", e.getMessage());
        }
    }

    /** Scenario 9 — CANCELLED: cancelled before fulfilment */
    private void seedOrderCancelled(OrderService orderService, List<ProductSummaryDTO> products) {
        try {
            var response = orderService.createOrder(buildOrderRequest(
                    "Irene Taylor", "irene.taylor@example.com", "+1-617-555-0109",
                    OrderSource.STOREFRONT,
                    "88 Newbury Street", "Boston", "MA", "02116", "USA",
                    new BigDecimal("8.99"), BigDecimal.ZERO, "Standard Shipping", null,
                    buildItems(products, 0, 1)));
            OrderCancelRequest cancelReq = new OrderCancelRequest();
            cancelReq.setReason("Customer requested cancellation — changed mind");
            orderService.cancelOrder(response.getId(), cancelReq);
            log.info("[Seed] CANCELLED order created — Irene Taylor");
        } catch (Exception e) {
            log.error("[Seed] CANCELLED order failed: {}", e.getMessage());
        }
    }

    /**
     * Scenario 10 — HIGH VALUE (auto-flagged): cross-border order exceeding $1 000
     * that triggers the risk scorer and sets isFlagged = true automatically.
     */
    private void seedOrderHighValue(OrderService orderService, List<ProductSummaryDTO> products) {
        try {
            List<OrderItemRequestDTO> items = new ArrayList<>();
            for (ProductSummaryDTO p : products) {
                items.add(OrderItemRequestDTO.builder()
                        .productId(p.getId())
                        .quantity(3)
                        .taxRate(new BigDecimal("0.08"))
                        .itemDiscount(BigDecimal.ZERO)
                        .build());
            }

            // Shipping country ≠ billing country — triggers the mismatch risk factor
            OrderAddressDTO shipping = address("James Anderson", "50 Rue de Rivoli",
                    "Paris", "Île-de-France", "75001", "France");
            OrderAddressDTO billing  = address("James Anderson", "500 5th Avenue",
                    "New York", "NY", "10110", "USA");

            orderService.createOrder(CreateOrderRequestDTO.builder()
                    .customerName("James Anderson")
                    .customerEmail("james.anderson@enterprise.com")
                    .customerPhone("+1-646-555-0110")
                    .source(OrderSource.API)
                    .shippingAddress(shipping)
                    .billingAddress(billing)
                    .shippingCost(new BigDecimal("49.99"))
                    .discountAmount(BigDecimal.ZERO)
                    .currency("USD")
                    .customerNotes("Priority enterprise delivery required")
                    .internalNotes("Auto-flagged for risk review — cross-border high-value order")
                    .items(items)
                    .build());
            log.info("[Seed] HIGH_VALUE (auto-flagged) order created — James Anderson");
        } catch (Exception e) {
            log.error("[Seed] HIGH_VALUE order failed: {}", e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // WAREHOUSE BUILDERS
    // ═════════════════════════════════════════════════════════════════════════

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

    // ═════════════════════════════════════════════════════════════════════════
    // INVENTORY BUILDER
    // ═════════════════════════════════════════════════════════════════════════

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

    // ═════════════════════════════════════════════════════════════════════════
    // ORDER BUILDER HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private CreateOrderRequestDTO buildOrderRequest(
            String name, String email, String phone,
            OrderSource source,
            String street, String city, String state, String postal, String country,
            BigDecimal shipping, BigDecimal discount,
            String shippingMethod, String coupon,
            List<OrderItemRequestDTO> items) {

        OrderAddressDTO addr = address(name, street, city, state, postal, country);
        return CreateOrderRequestDTO.builder()
                .customerName(name)
                .customerEmail(email)
                .customerPhone(phone)
                .source(source)
                .shippingAddress(addr)
                .billingAddress(addr)
                .shippingCost(shipping)
                .discountAmount(discount)
                .shippingMethod(shippingMethod)
                .couponCode(coupon)
                .currency("USD")
                .items(items)
                .build();
    }

    private OrderAddressDTO address(String name, String street,
                                    String city, String state,
                                    String postal, String country) {
        return OrderAddressDTO.builder()
                .fullName(name)
                .addressLine1(street)
                .city(city)
                .state(state)
                .postalCode(postal)
                .country(country)
                .build();
    }

    /**
     * Build a list of {@link OrderItemRequestDTO} from a slice of the product list.
     * Alternates quantity between 1 and 2 for variety.
     */
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

    // ═════════════════════════════════════════════════════════════════════════
    // PRODUCT BUILDER HELPERS
    // ═════════════════════════════════════════════════════════════════════════

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
                .cost(price.multiply(new BigDecimal("0.60")))   // 40 % gross margin
                .stock(stock)
                .lowStockThreshold(10)
                .status("active")
                .visibility("visible")
                .image(image)
                .gallery(gallery)
                .tags(tags)
                .featured(Math.random() > 0.70)                 // ~30 % chance featured
                .trackInventory(true)
                .allowBackorders(false)
                .vendor(brand)
                .productType("Physical")
                .attributes(attributes != null ? attributes : new ArrayList<>())
                .variants(variants != null ? variants : new ArrayList<>())
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // UTILITY HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Returns a safe sub-list capped at the actual list size.
     * Guarantees fromIndex and toIndex are always in bounds.
     */
    private List<ProductSummaryDTO> slice(List<ProductSummaryDTO> list, int from, int count, int total) {
        int safeFrom = Math.min(from, total - 1);
        int safeTo   = Math.min(safeFrom + count, total);
        return list.subList(safeFrom, safeTo);
    }

    /** Calculates a random quantity between [base × minFraction, base × maxFraction]. */
    private int proportional(int base, double minFraction, double maxFraction) {
        double fraction = minFraction + Math.random() * (maxFraction - minFraction);
        return Math.max(1, (int) (base * fraction));
    }

    /**
     * Unit cost = 60 % of selling price.
     * {@code ProductSummaryDTO} does not expose a cost field, so we derive it
     * consistently from price — matching the same 40 % margin baked in at
     * product creation time inside {@link #productWithVariants}.
     */
    private BigDecimal unitCost(BigDecimal price) {
        return price.multiply(new BigDecimal("0.60"));
    }
}