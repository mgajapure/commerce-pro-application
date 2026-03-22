package com.commerce_pro_backend.common.data;

import com.commerce_pro_backend.inventory.service.InventoryService;
import com.commerce_pro_backend.notification.dto.CreateNotificationRequest;
import com.commerce_pro_backend.notification.enums.NotificationChannel;
import com.commerce_pro_backend.notification.enums.NotificationPriority;
import com.commerce_pro_backend.notification.enums.NotificationType;
import com.commerce_pro_backend.notification.service.NotificationService;
import com.commerce_pro_backend.order.dto.OrderFilterDTO;
import com.commerce_pro_backend.order.service.OrderService;
import com.commerce_pro_backend.catalog.product.service.ProductService;
import com.commerce_pro_backend.user_identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.math.BigDecimal;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class NotificationDataSeeder {

    private final SeedDataLoader seedDataLoader;

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
                seedDataLoader.authenticateAsSuperAdmin(userDetailsService);

                var superadmin = userRepository.findByUsername("superadmin");
                if (superadmin.isEmpty()) {
                    log.warn("[Seed] No superadmin user — skipping notification seed");
                    return;
                }
                String userId = superadmin.get().getId();

                var orders = orderService.listOrders(new OrderFilterDTO(),
                        org.springframework.data.domain.PageRequest.of(0, 10));
                var products = productService.getAllProducts();
                var warehouses = inventoryService.getAllWarehouses();
                String warehouseName = warehouses.isEmpty() ? "Main Warehouse" : warehouses.get(0).getName();

                int created = 0;

                // Order-based notifications
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
                seedDataLoader.clearSecurityContext();
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
}
