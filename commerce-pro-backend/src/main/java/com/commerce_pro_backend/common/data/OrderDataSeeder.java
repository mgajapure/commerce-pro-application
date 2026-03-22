package com.commerce_pro_backend.common.data;

import com.commerce_pro_backend.catalog.product.dto.ProductSummaryDTO;
import com.commerce_pro_backend.catalog.product.service.ProductService;
import com.commerce_pro_backend.order.dto.*;
import com.commerce_pro_backend.order.enums.OrderSource;
import com.commerce_pro_backend.order.service.OrderService;
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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderDataSeeder {

    private final SeedDataLoader seedDataLoader;

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
                seedDataLoader.authenticateAsSuperAdmin(userDetailsService);
                log.info("[Seed] SecurityContext set to superadmin — order seeding can proceed");
            } catch (Exception e) {
                log.error("[Seed] Could not set SecurityContext: {}. Order seeding aborted.", e.getMessage());
                return;
            }

            try {
                JsonNode scenariosJson = seedDataLoader.loadTree("seed/order-scenarios.json");
                int total = products.size();

                for (JsonNode scenario : scenariosJson) {
                    String type = scenario.get("scenario").asText();
                    int custIdx = scenario.get("customerIndex").asInt() % dbCustomers.size();
                    int from = scenario.get("productFrom").asInt();
                    int count = scenario.get("productCount").asInt();
                    String coupon = scenario.has("coupon") ? scenario.get("coupon").asText() : null;

                    seedOrderScenario(orderService, type, dbCustomers.get(custIdx),
                            customerAddressRepository, slice(products, from, count, total), coupon);
                }

                log.info("[Seed] Orders complete — {} scenario orders created", scenariosJson.size());

                // Update customer stats from created orders
                updateCustomerStatsFromOrders(orderService, customerRepository, dbCustomers);
                log.info("[Seed] Customer stats (totalOrders, lifetimeSpend, lastOrder) updated");

            } finally {
                seedDataLoader.clearSecurityContext();
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

            var dbAddresses = addressRepo.findByCustomerId(customer.getId());
            OrderAddressDTO shippingAddress;
            OrderAddressDTO billingAddress;

            if (!dbAddresses.isEmpty()) {
                var shipAddr = dbAddresses.stream()
                        .filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                        .findFirst().orElse(dbAddresses.get(0));
                shippingAddress = toOrderAddress(shipAddr);

                var billAddr = dbAddresses.stream()
                        .filter(a -> a.getAddressType() == com.commerce_pro_backend.customer.enums.AddressType.BILLING
                                || a.getAddressType() == com.commerce_pro_backend.customer.enums.AddressType.BOTH)
                        .findFirst().orElse(shipAddr);
                billingAddress = toOrderAddress(billAddr);
            } else {
                shippingAddress = OrderAddressDTO.builder()
                        .fullName(name).addressLine1("123 Main St").city("New York")
                        .state("NY").postalCode("10001").country("USA").build();
                billingAddress = shippingAddress;
            }

            OrderSource source = switch (scenario) {
                case "ON_HOLD", "HIGH_VALUE" -> OrderSource.API;
                case "CLOSED" -> OrderSource.MANUAL;
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

            custOrders.stream()
                    .filter(o -> o.getCreatedAt() != null)
                    .max(java.util.Comparator.comparing(OrderSummaryDTO::getCreatedAt))
                    .ifPresent(latest -> {
                        customer.setLastOrderAt(latest.getCreatedAt());
                        customer.setLastOrderId(latest.getId());
                    });

            customer.setTier(customer.evaluateTier());
            customerRepository.save(customer);
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

    private List<ProductSummaryDTO> slice(List<ProductSummaryDTO> list, int from, int count, int total) {
        int safeFrom = Math.min(from, total - 1);
        int safeTo = Math.min(safeFrom + count, total);
        return list.subList(safeFrom, safeTo);
    }
}
