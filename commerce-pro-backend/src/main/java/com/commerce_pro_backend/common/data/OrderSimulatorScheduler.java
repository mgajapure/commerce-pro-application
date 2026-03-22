package com.commerce_pro_backend.common.data;

import com.commerce_pro_backend.catalog.product.dto.ProductSummaryDTO;
import com.commerce_pro_backend.catalog.product.service.ProductService;
import com.commerce_pro_backend.customer.entity.Customer;
import com.commerce_pro_backend.customer.entity.CustomerAddress;
import com.commerce_pro_backend.customer.repository.CustomerAddressRepository;
import com.commerce_pro_backend.customer.repository.CustomerRepository;
import com.commerce_pro_backend.notification.dto.CreateNotificationRequest;
import com.commerce_pro_backend.notification.enums.NotificationChannel;
import com.commerce_pro_backend.notification.enums.NotificationPriority;
import com.commerce_pro_backend.notification.enums.NotificationType;
import com.commerce_pro_backend.notification.service.NotificationService;
import com.commerce_pro_backend.order.dto.CreateOrderRequestDTO;
import com.commerce_pro_backend.order.dto.OrderAddressDTO;
import com.commerce_pro_backend.order.dto.OrderItemRequestDTO;
import com.commerce_pro_backend.order.dto.OrderResponseDTO;
import com.commerce_pro_backend.order.enums.OrderSource;
import com.commerce_pro_backend.order.service.OrderService;
import com.commerce_pro_backend.user_identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulates real-time order flow for development.
 * Every 60 seconds a new order is created and a notification is sent
 * to the superadmin user so the frontend can demonstrate live updates.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class OrderSimulatorScheduler {

    private final OrderService orderService;
    private final ProductService productService;
    private final NotificationService notificationService;
    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;

    private final AtomicInteger orderCounter = new AtomicInteger(100);

    private static final OrderSource[] SOURCES = {OrderSource.STOREFRONT, OrderSource.STOREFRONT, OrderSource.API, OrderSource.MANUAL};

    @Transactional
    @Scheduled(fixedDelay = 60_000, initialDelay = 90_000) // every 60s, start 90s after boot
    public void simulateNewOrder() {
        try {
            List<ProductSummaryDTO> products = productService.getAllProducts();
            if (products.isEmpty()) return;

            List<Customer> customers = customerRepository.findAll(PageRequest.of(0, 50)).getContent();
            if (customers.isEmpty()) return;

            // Authenticate as superadmin for order creation
            var userDetails = userDetailsService.loadUserByUsername("superadmin");
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

            try {
                // Pick random customer from database
                ThreadLocalRandom rng = ThreadLocalRandom.current();
                Customer customer = customers.get(rng.nextInt(customers.size()));
                String customerName = customer.getFirstName() + " " + customer.getLastName();

                // Pick 1-3 random products
                int itemCount = rng.nextInt(1, 4);
                List<OrderItemRequestDTO> items = new java.util.ArrayList<>();
                for (int i = 0; i < itemCount; i++) {
                    ProductSummaryDTO product = products.get(rng.nextInt(products.size()));
                    items.add(OrderItemRequestDTO.builder()
                            .productId(product.getId())
                            .quantity(rng.nextInt(1, 4))
                            .taxRate(new BigDecimal("0.08"))
                            .itemDiscount(BigDecimal.ZERO)
                            .build());
                }

                // Use customer's address from DB
                OrderAddressDTO addr;
                List<CustomerAddress> addresses = customerAddressRepository.findByCustomerId(customer.getId());
                if (!addresses.isEmpty()) {
                    CustomerAddress ca = addresses.get(rng.nextInt(addresses.size()));
                    addr = OrderAddressDTO.builder()
                            .fullName(ca.getFullName())
                            .addressLine1(ca.getAddressLine1())
                            .addressLine2(ca.getAddressLine2())
                            .city(ca.getCity())
                            .state(ca.getState())
                            .postalCode(ca.getPostalCode())
                            .country(ca.getCountry())
                            .phone(ca.getPhone())
                            .build();
                } else {
                    addr = OrderAddressDTO.builder()
                            .fullName(customerName)
                            .addressLine1("123 Main St").city("New York").state("NY")
                            .postalCode("10001").country("USA")
                            .build();
                }

                BigDecimal shippingCost = new BigDecimal(rng.nextInt(5, 20) + ".99");
                OrderSource source = SOURCES[rng.nextInt(SOURCES.length)];

                OrderResponseDTO order = orderService.createOrder(CreateOrderRequestDTO.builder()
                        .customerId(customer.getId())
                        .customerName(customerName)
                        .customerEmail(customer.getEmail())
                        .customerPhone(customer.getPhone())
                        .source(source)
                        .shippingAddress(addr)
                        .billingAddress(addr)
                        .shippingCost(shippingCost)
                        .discountAmount(BigDecimal.ZERO)
                        .currency("USD")
                        .shippingMethod(rng.nextBoolean() ? "Standard Shipping" : "Express Shipping")
                        .items(items)
                        .build());

                // Update customer stats
                customer.setTotalOrders((customer.getTotalOrders() != null ? customer.getTotalOrders() : 0) + 1);
                BigDecimal prevSpend = customer.getLifetimeSpend() != null ? customer.getLifetimeSpend() : BigDecimal.ZERO;
                customer.setLifetimeSpend(prevSpend.add(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO));
                customer.recalculateAverageOrderValue();
                customer.setLastOrderAt(order.getCreatedAt());
                customer.setLastOrderId(order.getId());
                customer.setTier(customer.evaluateTier());
                customerRepository.save(customer);

                int num = orderCounter.incrementAndGet();
                log.info("[Simulator] Order #{} created — {} ({} items, {})",
                        num, customerName, itemCount, source);

                // Send notification to superadmin
                var superadmin = userRepository.findByUsername("superadmin");
                if (superadmin.isPresent()) {
                    String userId = superadmin.get().getId();

                    notificationService.createNotification(CreateNotificationRequest.builder()
                            .userId(userId)
                            .title("New Order from " + customerName)
                            .message("Order " + order.getOrderNumber() + " — " + itemCount
                                    + " item(s), $" + order.getTotalAmount() + " via " + source)
                            .type(NotificationType.ORDER_UPDATE)
                            .channel(NotificationChannel.IN_APP)
                            .priority(order.getTotalAmount().compareTo(new BigDecimal("500")) > 0
                                    ? NotificationPriority.HIGH : NotificationPriority.NORMAL)
                            .link("/orders")
                            .build());
                }

            } finally {
                SecurityContextHolder.clearContext();
            }

        } catch (Exception e) {
            log.warn("[Simulator] Order simulation failed: {}", e.getMessage());
        }
    }
}
