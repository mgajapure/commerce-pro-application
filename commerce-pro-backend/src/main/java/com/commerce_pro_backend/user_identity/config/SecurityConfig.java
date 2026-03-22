package com.commerce_pro_backend.user_identity.config;

import com.commerce_pro_backend.user_identity.service.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.commerce_pro_backend.user_identity.service.JwtAuthenticationFilter;
import com.commerce_pro_backend.user_identity.service.JwtTokenProvider;
import com.commerce_pro_backend.user_identity.service.SuperAdminAuthorizationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final SuperAdminAuthorizationFilter superAdminFilter;
    private final RateLimitFilter rateLimitFilter;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/h2-console/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                // Disable CSRF for H2 Console and stateless JWT
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**")
                        .disable())
                // Configure headers before authorizeHttpRequests
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                        .contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors 'self'")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - H2 Console must be first
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/v1/auth/login", "/v1/auth/refresh").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Super admin endpoints
                        .requestMatchers("/v1/admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/v1/identity/config/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/v1/identity/audit/**").hasAnyRole("SUPER_ADMIN", "AUDIT_ADMIN")

                        // Identity User
                        .requestMatchers(HttpMethod.POST, "/v1/identity/users").hasAuthority("identity:user:create")
                        .requestMatchers(HttpMethod.DELETE, "/v1/identity/users/**").hasAuthority("identity:user:delete")

                        // Identity roles (align with RoleController + PermissionRegistry)
                        .requestMatchers(HttpMethod.GET, "/v1/identity/roles/**").hasAuthority("identity:role:read")
                        .requestMatchers(HttpMethod.POST, "/v1/identity/roles").hasAuthority("identity:role:create")
                        .requestMatchers(HttpMethod.PUT, "/v1/identity/roles/**").hasAuthority("identity:role:update")
                        .requestMatchers(HttpMethod.DELETE, "/v1/identity/roles/**").hasAuthority("identity:role:delete")
                        .requestMatchers(HttpMethod.POST, "/v1/identity/roles/*/permissions").hasAuthority("identity:role:manage-permissions")
                        .requestMatchers(HttpMethod.DELETE, "/v1/identity/roles/*/permissions").hasAuthority("identity:role:manage-permissions")
                        .requestMatchers(HttpMethod.POST, "/v1/identity/roles/*/hierarchy/parent").hasAuthority("identity:role:manage-hierarchy")

                        // Identity permissions (align with PermissionController)
                        .requestMatchers(HttpMethod.GET, "/v1/identity/permissions/**").hasAuthority("identity:permission:read")
                        .requestMatchers(HttpMethod.POST, "/v1/identity/permissions/**").hasAuthority("identity:permission:create")
                        .requestMatchers(HttpMethod.PUT, "/v1/identity/permissions/**").hasAuthority("identity:permission:update")
                        .requestMatchers(HttpMethod.DELETE, "/v1/identity/permissions/**").hasAuthority("identity:permission:delete")

                        // Order Management  (align with OrderController)
                        .requestMatchers(HttpMethod.GET,    "/v1/orders/**").hasAuthority("order:order:read")
                        .requestMatchers(HttpMethod.GET,    "/v1/orders").hasAuthority("order:order:read")
                        .requestMatchers(HttpMethod.POST,   "/v1/orders").hasAuthority("order:order:create")
                        .requestMatchers(HttpMethod.PUT,    "/v1/orders/**").hasAuthority("order:order:update")
                        .requestMatchers(HttpMethod.PATCH,  "/v1/orders/*/tracking").hasAuthority("order:order:update")
                        .requestMatchers(HttpMethod.DELETE, "/v1/orders/**").hasAuthority("order:order:cancel")
                        .requestMatchers(HttpMethod.POST,   "/v1/orders/*/confirm").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/v1/orders/*/process").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/v1/orders/*/ship").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/v1/orders/*/deliver").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/v1/orders/*/hold").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/v1/orders/*/release-hold").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/v1/orders/*/close").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/v1/orders/bulk-action").hasAuthority("order:order:bulk-action")

                        // Fulfillment — Pick Lists & Queue
                        .requestMatchers(HttpMethod.GET,    "/v1/fulfillment/queue").hasAuthority("fulfillment:picklist:read")
                        .requestMatchers(HttpMethod.GET,    "/v1/fulfillment/stats").hasAuthority("fulfillment:stats")
                        .requestMatchers(HttpMethod.GET,    "/v1/fulfillment/pick-lists/**").hasAuthority("fulfillment:picklist:read")
                        .requestMatchers(HttpMethod.GET,    "/v1/fulfillment/pick-lists").hasAuthority("fulfillment:picklist:read")
                        .requestMatchers(HttpMethod.POST,   "/v1/fulfillment/pick-lists").hasAuthority("fulfillment:picklist:create")
                        .requestMatchers(HttpMethod.POST,   "/v1/fulfillment/pick-lists/*/assign").hasAuthority("fulfillment:picklist:manage")
                        .requestMatchers(HttpMethod.POST,   "/v1/fulfillment/pick-lists/*/start").hasAuthority("fulfillment:picklist:manage")
                        .requestMatchers(HttpMethod.PATCH,  "/v1/fulfillment/pick-lists/*/items/*").hasAuthority("fulfillment:picklist:manage")
                        .requestMatchers(HttpMethod.POST,   "/v1/fulfillment/pick-lists/*/complete").hasAuthority("fulfillment:picklist:manage")
                        .requestMatchers(HttpMethod.POST,   "/v1/fulfillment/pick-lists/*/cancel").hasAuthority("fulfillment:picklist:manage")
                        // Fulfillment — Carriers
                        .requestMatchers(HttpMethod.GET,    "/v1/fulfillment/carriers/**").hasAuthority("fulfillment:carrier:read")
                        .requestMatchers(HttpMethod.GET,    "/v1/fulfillment/carriers").hasAuthority("fulfillment:carrier:read")
                        .requestMatchers(HttpMethod.POST,   "/v1/fulfillment/carriers").hasAuthority("fulfillment:carrier:manage")
                        .requestMatchers(HttpMethod.PUT,    "/v1/fulfillment/carriers/**").hasAuthority("fulfillment:carrier:manage")
                        .requestMatchers(HttpMethod.DELETE, "/v1/fulfillment/carriers/**").hasAuthority("fulfillment:carrier:manage")
                        // Fulfillment — Shipping Rules
                        .requestMatchers(HttpMethod.GET,    "/v1/fulfillment/shipping-rules/**").hasAuthority("fulfillment:rules:read")
                        .requestMatchers(HttpMethod.GET,    "/v1/fulfillment/shipping-rules").hasAuthority("fulfillment:rules:read")
                        .requestMatchers(HttpMethod.POST,   "/v1/fulfillment/shipping-rules").hasAuthority("fulfillment:rules:manage")
                        .requestMatchers(HttpMethod.PUT,    "/v1/fulfillment/shipping-rules/**").hasAuthority("fulfillment:rules:manage")
                        .requestMatchers(HttpMethod.DELETE, "/v1/fulfillment/shipping-rules/**").hasAuthority("fulfillment:rules:manage")
                        // Shipments
                        .requestMatchers(HttpMethod.GET,    "/v1/shipments/**").hasAuthority("fulfillment:shipment:read")
                        .requestMatchers(HttpMethod.GET,    "/v1/shipments").hasAuthority("fulfillment:shipment:read")
                        .requestMatchers(HttpMethod.POST,   "/v1/shipments").hasAuthority("fulfillment:shipment:create")
                        .requestMatchers(HttpMethod.PUT,    "/v1/shipments/**").hasAuthority("fulfillment:shipment:update")
                        .requestMatchers(HttpMethod.POST,   "/v1/shipments/*/tracking-events").hasAuthority("fulfillment:shipment:update")
                        .requestMatchers(HttpMethod.POST,   "/v1/shipments/*/deliver").hasAuthority("fulfillment:shipment:manage")
                        .requestMatchers(HttpMethod.DELETE, "/v1/shipments/**").hasAuthority("fulfillment:shipment:delete")

                        // Customer Management
                        .requestMatchers(HttpMethod.GET,    "/v1/customers").hasAuthority("customer:customer:read")
                        .requestMatchers(HttpMethod.GET,    "/v1/customers/**").hasAuthority("customer:customer:read")
                        .requestMatchers(HttpMethod.POST,   "/v1/customers").hasAuthority("customer:customer:create")
                        .requestMatchers(HttpMethod.PUT,    "/v1/customers/**").hasAuthority("customer:customer:update")
                        .requestMatchers(HttpMethod.DELETE, "/v1/customers/**").hasAuthority("customer:customer:delete")
                        .requestMatchers(HttpMethod.POST,   "/v1/customers/*/blacklist").hasAuthority("customer:customer:blacklist")
                        .requestMatchers(HttpMethod.POST,   "/v1/customers/*/unblacklist").hasAuthority("customer:customer:blacklist")
                        .requestMatchers(HttpMethod.POST,   "/v1/customers/*/fraud-flag").hasAuthority("customer:customer:fraud-flag")
                        .requestMatchers(HttpMethod.POST,   "/v1/customers/*/fraud-resolve").hasAuthority("customer:customer:fraud-flag")
                        .requestMatchers(HttpMethod.POST,   "/v1/customers/*/loyalty/adjust").hasAuthority("customer:customer:manage-loyalty")
                        .requestMatchers(HttpMethod.POST,   "/v1/customers/*/evaluate-tier").hasAuthority("customer:customer:manage-tier")
                        .requestMatchers(HttpMethod.POST,   "/v1/customers/*/status").hasAuthority("customer:customer:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/v1/customers/*/communications").hasAuthority("customer:communication:create")
                        
                        // Customer Groups
                        .requestMatchers(HttpMethod.GET,    "/v1/customer-groups/**").hasAuthority("customer:group:read")
                        .requestMatchers(HttpMethod.GET,    "/v1/customer-groups").hasAuthority("customer:group:read")
                        .requestMatchers(HttpMethod.POST,   "/v1/customer-groups").hasAuthority("customer:group:create")
                        .requestMatchers(HttpMethod.PUT,    "/v1/customer-groups/**").hasAuthority("customer:group:update")
                        .requestMatchers(HttpMethod.DELETE, "/v1/customer-groups/**").hasAuthority("customer:group:delete")

                        // Analytics & Reporting
                        .requestMatchers(HttpMethod.GET,  "/v1/analytics/dashboard").hasAuthority("analytics:dashboard:read")
                        .requestMatchers(HttpMethod.POST, "/v1/analytics/sales/**").hasAuthority("analytics:sales:read")
                        .requestMatchers(HttpMethod.POST, "/v1/analytics/inventory/**").hasAuthority("analytics:inventory:read")
                        .requestMatchers(HttpMethod.POST, "/v1/analytics/orders/**").hasAuthority("analytics:orders:read")
                        .requestMatchers(HttpMethod.POST, "/v1/analytics/customers/**").hasAuthority("analytics:customers:read")
                        .requestMatchers(HttpMethod.POST, "/v1/analytics/financial/**").hasAuthority("analytics:financial:read")
                        .requestMatchers(HttpMethod.POST, "/v1/analytics/shipping/**").hasAuthority("analytics:shipping:read")
                        .requestMatchers(HttpMethod.POST, "/v1/analytics/returns/**").hasAuthority("analytics:returns:read")
                        .requestMatchers(HttpMethod.GET,  "/v1/analytics/reports/saved/**").hasAuthority("analytics:reports:read")
                        .requestMatchers(HttpMethod.GET,  "/v1/analytics/reports/saved").hasAuthority("analytics:reports:read")
                        .requestMatchers(HttpMethod.POST, "/v1/analytics/reports/saved").hasAuthority("analytics:reports:manage")
                        .requestMatchers(HttpMethod.PUT,  "/v1/analytics/reports/saved/**").hasAuthority("analytics:reports:manage")
                        .requestMatchers(HttpMethod.DELETE,"/v1/analytics/reports/saved/**").hasAuthority("analytics:reports:manage")
                        .requestMatchers(HttpMethod.POST, "/v1/analytics/reports/saved/*/run").hasAuthority("analytics:reports:run")
                        .requestMatchers(HttpMethod.POST, "/v1/analytics/reports/run").hasAuthority("analytics:reports:run")
                        .requestMatchers(HttpMethod.GET,  "/v1/analytics/reports/scheduled/**").hasAuthority("analytics:reports:read")
                        .requestMatchers(HttpMethod.GET,  "/v1/analytics/reports/scheduled").hasAuthority("analytics:reports:read")
                        .requestMatchers(HttpMethod.POST, "/v1/analytics/reports/scheduled").hasAuthority("analytics:reports:manage")
                        .requestMatchers(HttpMethod.PUT,  "/v1/analytics/reports/scheduled/**").hasAuthority("analytics:reports:manage")
                        .requestMatchers(HttpMethod.DELETE,"/v1/analytics/reports/scheduled/**").hasAuthority("analytics:reports:manage")
                        .requestMatchers(HttpMethod.POST, "/v1/analytics/reports/scheduled/*/toggle").hasAuthority("analytics:reports:manage")
                        .requestMatchers(HttpMethod.POST, "/v1/analytics/reports/scheduled/*/run-now").hasAuthority("analytics:reports:run")
                        .requestMatchers(HttpMethod.GET,  "/v1/analytics/reports/executions/**").hasAuthority("analytics:reports:read")
                        .requestMatchers(HttpMethod.GET,  "/v1/analytics/reports/executions").hasAuthority("analytics:reports:read")

                        // Inventory Management
                        // Permissions registered (inventory:read, inventory:write) — assign to roles via Identity UI
                        .requestMatchers("/v1/inventory/**").authenticated()

                        // Catalog — Brands, Categories, Products, Collections, Attributes, Reviews, SEO
                        // Permissions registered (catalog:read, catalog:write) — assign to roles via Identity UI
                        .requestMatchers("/v1/brands/**").authenticated()
                        .requestMatchers("/v1/categories/**").authenticated()
                        .requestMatchers("/v1/products/**").authenticated()
                        .requestMatchers("/v1/collections/**").authenticated()
                        .requestMatchers("/v1/attributes/**").authenticated()
                        .requestMatchers("/v1/reviews/**").authenticated()
                        .requestMatchers("/v1/seo/**").authenticated()

                        // ── Payment & Finance ──────────────────────────────────────────────────
                        // Transactions
                        .requestMatchers(HttpMethod.GET,    "/v1/payments/transactions/**").hasAuthority("payment:transaction:read")
                        .requestMatchers(HttpMethod.GET,    "/v1/payments/transactions").hasAuthority("payment:transaction:read")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/transactions").hasAuthority("payment:transaction:create")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/transactions/*/capture").hasAuthority("payment:transaction:capture")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/transactions/*/void").hasAuthority("payment:transaction:void")
                        .requestMatchers(HttpMethod.PATCH,  "/v1/payments/transactions/*/flag").hasAuthority("payment:transaction:flag")

                        // Pending payments
                        .requestMatchers(HttpMethod.GET,    "/v1/payments/pending/**").hasAuthority("payment:transaction:read")
                        .requestMatchers(HttpMethod.GET,    "/v1/payments/pending").hasAuthority("payment:transaction:read")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/pending/*/capture").hasAuthority("payment:transaction:capture")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/pending/*/void").hasAuthority("payment:transaction:void")

                        // Refunds
                        .requestMatchers(HttpMethod.GET,    "/v1/payments/refunds/**").hasAuthority("payment:refund:read")
                        .requestMatchers(HttpMethod.GET,    "/v1/payments/refunds").hasAuthority("payment:refund:read")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/refunds").hasAuthority("payment:refund:request")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/refunds/*/approve").hasAuthority("payment:refund:approve")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/refunds/*/reject").hasAuthority("payment:refund:approve")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/refunds/*/process").hasAuthority("payment:refund:process")

                        // Chargebacks
                        .requestMatchers(HttpMethod.GET,    "/v1/payments/chargebacks/**").hasAuthority("payment:chargeback:read")
                        .requestMatchers(HttpMethod.GET,    "/v1/payments/chargebacks").hasAuthority("payment:chargeback:read")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/chargebacks").hasAuthority("payment:chargeback:manage")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/chargebacks/**").hasAuthority("payment:chargeback:manage")
                        .requestMatchers(HttpMethod.PUT,    "/v1/payments/chargebacks/**").hasAuthority("payment:chargeback:manage")

                        // Reconciliation
                        .requestMatchers(HttpMethod.GET,    "/v1/payments/reconciliation/**").hasAuthority("payment:reconciliation:read")
                        .requestMatchers(HttpMethod.GET,    "/v1/payments/reconciliation").hasAuthority("payment:reconciliation:read")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/reconciliation/**").hasAuthority("payment:reconciliation:manage")

                        // Payouts
                        .requestMatchers(HttpMethod.GET,    "/v1/payments/payouts/**").hasAuthority("payment:payout:read")
                        .requestMatchers(HttpMethod.GET,    "/v1/payments/payouts").hasAuthority("payment:payout:read")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/payouts").hasAuthority("payment:payout:create")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/payouts/*/approve").hasAuthority("payment:payout:approve")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/payouts/*/cancel").hasAuthority("payment:payout:approve")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/payouts/*/initiate").hasAuthority("payment:payout:process")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/payouts/*/complete").hasAuthority("payment:payout:process")

                        // Payment Links — public slug endpoints first (no auth required)
                        .requestMatchers(HttpMethod.GET,    "/v1/payments/links/slug/**").permitAll()
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/links/slug/**").permitAll()
                        .requestMatchers(HttpMethod.GET,    "/v1/payments/links/**").hasAuthority("payment:link:read")
                        .requestMatchers(HttpMethod.GET,    "/v1/payments/links").hasAuthority("payment:link:read")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/links").hasAuthority("payment:link:create")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/links/*/deactivate").hasAuthority("payment:link:create")

                        // Payment Methods
                        .requestMatchers(HttpMethod.GET,    "/v1/payments/methods/**").hasAuthority("payment:method:read")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/methods/**").hasAuthority("payment:method:manage")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/methods").hasAuthority("payment:method:manage")
                        .requestMatchers(HttpMethod.DELETE, "/v1/payments/methods/**").hasAuthority("payment:method:manage")

                        // Gateway Configuration (high risk — requires payment:gateway:manage)
                        .requestMatchers(HttpMethod.GET,    "/v1/payments/gateways/**").hasAuthority("payment:gateway:read")
                        .requestMatchers(HttpMethod.GET,    "/v1/payments/gateways").hasAuthority("payment:gateway:read")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/gateways/**").hasAuthority("payment:gateway:manage")
                        .requestMatchers(HttpMethod.POST,   "/v1/payments/gateways").hasAuthority("payment:gateway:manage")
                        .requestMatchers(HttpMethod.PUT,    "/v1/payments/gateways/**").hasAuthority("payment:gateway:manage")

                        // ── Finance Module ────────────────────────────────────────────────────
                        // Revenue & P&L
                        .requestMatchers(HttpMethod.GET, "/v1/finance/revenue/**").hasAuthority("finance:revenue:read")
                        .requestMatchers(HttpMethod.GET, "/v1/finance/pnl/**").hasAuthority("finance:pnl:read")
                        // Cash Flow
                        .requestMatchers(HttpMethod.GET, "/v1/finance/cashflow/**").hasAuthority("finance:cashflow:read")
                        // Exchange Rates
                        .requestMatchers(HttpMethod.GET, "/v1/finance/exchange-rates/**").hasAuthority("finance:rates:read")
                        .requestMatchers(HttpMethod.GET, "/v1/finance/exchange-rates").hasAuthority("finance:rates:read")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/exchange-rates/**").hasAuthority("finance:rates:manage")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/exchange-rates").hasAuthority("finance:rates:manage")
                        // Financial Periods
                        .requestMatchers(HttpMethod.GET, "/v1/finance/periods/**").hasAuthority("finance:period:read")
                        .requestMatchers(HttpMethod.GET, "/v1/finance/periods").hasAuthority("finance:period:read")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/periods").hasAuthority("finance:period:manage")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/periods/*/close").hasAuthority("finance:period:close")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/periods/*/lock").hasAuthority("finance:period:close")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/periods/*/reopen").hasAuthority("finance:period:close")
                        // Vendors
                        .requestMatchers(HttpMethod.GET, "/v1/finance/vendors/**").hasAuthority("finance:vendor:read")
                        .requestMatchers(HttpMethod.GET, "/v1/finance/vendors").hasAuthority("finance:vendor:read")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/vendors").hasAuthority("finance:vendor:manage")
                        .requestMatchers(HttpMethod.PUT, "/v1/finance/vendors/**").hasAuthority("finance:vendor:manage")
                        .requestMatchers(HttpMethod.PATCH, "/v1/finance/vendors/**").hasAuthority("finance:vendor:manage")
                        // Customer Invoices (AR)
                        .requestMatchers(HttpMethod.GET, "/v1/finance/invoices/customer/**").hasAuthority("finance:invoice:read")
                        .requestMatchers(HttpMethod.GET, "/v1/finance/invoices/customer").hasAuthority("finance:invoice:read")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/invoices/customer").hasAuthority("finance:invoice:create")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/invoices/customer/*/send").hasAuthority("finance:invoice:create")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/invoices/customer/*/record-payment").hasAuthority("finance:invoice:payment")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/invoices/customer/*/void").hasAuthority("finance:invoice:void")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/invoices/customer/*/write-off").hasAuthority("finance:invoice:void")
                        // Vendor Invoices (AP)
                        .requestMatchers(HttpMethod.GET, "/v1/finance/invoices/vendor/**").hasAuthority("finance:ap:read")
                        .requestMatchers(HttpMethod.GET, "/v1/finance/invoices/vendor").hasAuthority("finance:ap:read")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/invoices/vendor").hasAuthority("finance:ap:create")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/invoices/vendor/*/approve").hasAuthority("finance:ap:approve")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/invoices/vendor/*/reject").hasAuthority("finance:ap:approve")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/invoices/vendor/*/schedule-payment").hasAuthority("finance:ap:approve")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/invoices/vendor/*/record-payment").hasAuthority("finance:ap:pay")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/invoices/vendor/*/dispute").hasAuthority("finance:ap:approve")
                        // Tax
                        .requestMatchers(HttpMethod.GET, "/v1/finance/tax/**").hasAuthority("finance:tax:read")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/tax/**").hasAuthority("finance:tax:manage")
                        .requestMatchers(HttpMethod.PUT, "/v1/finance/tax/**").hasAuthority("finance:tax:manage")
                        .requestMatchers(HttpMethod.PATCH, "/v1/finance/tax/**").hasAuthority("finance:tax:manage")
                        // Expenses
                        .requestMatchers(HttpMethod.GET, "/v1/finance/expenses/**").hasAuthority("finance:expense:read")
                        .requestMatchers(HttpMethod.GET, "/v1/finance/expenses").hasAuthority("finance:expense:read")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/expenses").hasAuthority("finance:expense:create")
                        .requestMatchers(HttpMethod.PUT, "/v1/finance/expenses/**").hasAuthority("finance:expense:create")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/expenses/*/approve").hasAuthority("finance:expense:approve")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/expenses/*/reject").hasAuthority("finance:expense:approve")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/expenses/*/mark-paid").hasAuthority("finance:expense:approve")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/expenses/categories").hasAuthority("finance:expense:manage")
                        .requestMatchers(HttpMethod.PUT, "/v1/finance/expenses/categories/**").hasAuthority("finance:expense:manage")
                        // Journal Entries
                        .requestMatchers(HttpMethod.GET, "/v1/finance/journal/**").hasAuthority("finance:journal:read")
                        .requestMatchers(HttpMethod.GET, "/v1/finance/journal").hasAuthority("finance:journal:read")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/journal").hasAuthority("finance:journal:post")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/journal/*/reverse").hasAuthority("finance:journal:post")
                        // Budgets
                        .requestMatchers(HttpMethod.GET, "/v1/finance/budgets/**").hasAuthority("finance:budget:read")
                        .requestMatchers(HttpMethod.GET, "/v1/finance/budgets").hasAuthority("finance:budget:read")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/budgets").hasAuthority("finance:budget:manage")
                        .requestMatchers(HttpMethod.PUT, "/v1/finance/budgets/**").hasAuthority("finance:budget:manage")
                        .requestMatchers(HttpMethod.POST, "/v1/finance/budgets/*/approve").hasAuthority("finance:budget:approve")

                        // Dashboard
                        // Permission registered (dashboard:read) — assign to roles via Identity UI
                        .requestMatchers("/v1/dashboard/**").authenticated()

                        // Notifications
                        // Permissions registered (notification:read, notification:write) — assign to roles via Identity UI
                        .requestMatchers("/v1/notifications/**").authenticated()

                        // File Storage
                        .requestMatchers("/v1/files/**").authenticated()

                        // All other requests need authentication
                        .anyRequest().authenticated())
                .addFilterBefore(superAdminFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                // Rate limit runs first — before JWT auth so locked-out IPs never reach token parsing
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}