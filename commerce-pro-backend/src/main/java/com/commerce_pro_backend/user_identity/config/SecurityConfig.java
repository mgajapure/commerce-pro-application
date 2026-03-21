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
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Super admin endpoints
                        .requestMatchers("/api/v1/admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/v1/identity/config/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/v1/identity/audit/**").hasAnyRole("SUPER_ADMIN", "AUDIT_ADMIN")

                        // Identity User
                        .requestMatchers(HttpMethod.POST, "/api/v1/identity/users").hasAuthority("identity:user:create")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/identity/users/**").hasAuthority("identity:user:delete")

                        // Identity roles (align with RoleController + PermissionRegistry)
                        .requestMatchers(HttpMethod.GET, "/api/v1/identity/roles/**").hasAuthority("identity:role:read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/identity/roles").hasAuthority("identity:role:create")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/identity/roles/**").hasAuthority("identity:role:update")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/identity/roles/**").hasAuthority("identity:role:delete")
                        .requestMatchers(HttpMethod.POST, "/api/v1/identity/roles/*/permissions").hasAuthority("identity:role:manage-permissions")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/identity/roles/*/permissions").hasAuthority("identity:role:manage-permissions")
                        .requestMatchers(HttpMethod.POST, "/api/v1/identity/roles/*/hierarchy/parent").hasAuthority("identity:role:manage-hierarchy")

                        // Identity permissions (align with PermissionController)
                        .requestMatchers(HttpMethod.GET, "/api/v1/identity/permissions/**").hasAuthority("identity:permission:read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/identity/permissions/**").hasAuthority("identity:permission:create")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/identity/permissions/**").hasAuthority("identity:permission:update")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/identity/permissions/**").hasAuthority("identity:permission:delete")

                        // Order Management  (align with OrderController)
                        .requestMatchers(HttpMethod.GET,    "/api/v1/orders/**").hasAuthority("order:order:read")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/orders").hasAuthority("order:order:read")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/orders").hasAuthority("order:order:create")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/orders/**").hasAuthority("order:order:update")
                        .requestMatchers(HttpMethod.PATCH,  "/api/v1/orders/*/tracking").hasAuthority("order:order:update")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/orders/**").hasAuthority("order:order:cancel")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/orders/*/confirm").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/orders/*/process").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/orders/*/ship").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/orders/*/deliver").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/orders/*/hold").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/orders/*/release-hold").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/orders/*/close").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/orders/bulk-action").hasAuthority("order:order:bulk-action")

                        // Fulfillment — Pick Lists & Queue
                        .requestMatchers(HttpMethod.GET,    "/api/v1/fulfillment/queue").hasAuthority("fulfillment:picklist:read")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/fulfillment/stats").hasAuthority("fulfillment:stats")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/fulfillment/pick-lists/**").hasAuthority("fulfillment:picklist:read")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/fulfillment/pick-lists").hasAuthority("fulfillment:picklist:read")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/fulfillment/pick-lists").hasAuthority("fulfillment:picklist:create")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/fulfillment/pick-lists/*/assign").hasAuthority("fulfillment:picklist:manage")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/fulfillment/pick-lists/*/start").hasAuthority("fulfillment:picklist:manage")
                        .requestMatchers(HttpMethod.PATCH,  "/api/v1/fulfillment/pick-lists/*/items/*").hasAuthority("fulfillment:picklist:manage")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/fulfillment/pick-lists/*/complete").hasAuthority("fulfillment:picklist:manage")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/fulfillment/pick-lists/*/cancel").hasAuthority("fulfillment:picklist:manage")
                        // Fulfillment — Carriers
                        .requestMatchers(HttpMethod.GET,    "/api/v1/fulfillment/carriers/**").hasAuthority("fulfillment:carrier:read")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/fulfillment/carriers").hasAuthority("fulfillment:carrier:read")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/fulfillment/carriers").hasAuthority("fulfillment:carrier:manage")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/fulfillment/carriers/**").hasAuthority("fulfillment:carrier:manage")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/fulfillment/carriers/**").hasAuthority("fulfillment:carrier:manage")
                        // Fulfillment — Shipping Rules
                        .requestMatchers(HttpMethod.GET,    "/api/v1/fulfillment/shipping-rules/**").hasAuthority("fulfillment:rules:read")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/fulfillment/shipping-rules").hasAuthority("fulfillment:rules:read")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/fulfillment/shipping-rules").hasAuthority("fulfillment:rules:manage")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/fulfillment/shipping-rules/**").hasAuthority("fulfillment:rules:manage")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/fulfillment/shipping-rules/**").hasAuthority("fulfillment:rules:manage")
                        // Shipments
                        .requestMatchers(HttpMethod.GET,    "/api/v1/shipments/**").hasAuthority("fulfillment:shipment:read")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/shipments").hasAuthority("fulfillment:shipment:read")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/shipments").hasAuthority("fulfillment:shipment:create")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/shipments/**").hasAuthority("fulfillment:shipment:update")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/shipments/*/tracking-events").hasAuthority("fulfillment:shipment:update")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/shipments/*/deliver").hasAuthority("fulfillment:shipment:manage")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/shipments/**").hasAuthority("fulfillment:shipment:delete")

                        // Customer Management
                        .requestMatchers(HttpMethod.GET,    "/api/v1/customers").hasAuthority("customer:customer:read")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/customers/**").hasAuthority("customer:customer:read")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/customers").hasAuthority("customer:customer:create")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/customers/**").hasAuthority("customer:customer:update")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/customers/**").hasAuthority("customer:customer:delete")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/customers/*/blacklist").hasAuthority("customer:customer:blacklist")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/customers/*/unblacklist").hasAuthority("customer:customer:blacklist")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/customers/*/fraud-flag").hasAuthority("customer:customer:fraud-flag")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/customers/*/fraud-resolve").hasAuthority("customer:customer:fraud-flag")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/customers/*/loyalty/adjust").hasAuthority("customer:customer:manage-loyalty")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/customers/*/evaluate-tier").hasAuthority("customer:customer:manage-tier")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/customers/*/status").hasAuthority("customer:customer:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/customers/*/communications").hasAuthority("customer:communication:create")
                        
                        // Customer Groups
                        .requestMatchers(HttpMethod.GET,    "/api/v1/customer-groups/**").hasAuthority("customer:group:read")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/customer-groups").hasAuthority("customer:group:read")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/customer-groups").hasAuthority("customer:group:create")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/customer-groups/**").hasAuthority("customer:group:update")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/customer-groups/**").hasAuthority("customer:group:delete")

                        // Analytics & Reporting
                        .requestMatchers(HttpMethod.GET,  "/api/v1/analytics/dashboard").hasAuthority("analytics:dashboard:read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/analytics/sales/**").hasAuthority("analytics:sales:read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/analytics/inventory/**").hasAuthority("analytics:inventory:read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/analytics/orders/**").hasAuthority("analytics:orders:read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/analytics/customers/**").hasAuthority("analytics:customers:read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/analytics/financial/**").hasAuthority("analytics:financial:read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/analytics/shipping/**").hasAuthority("analytics:shipping:read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/analytics/returns/**").hasAuthority("analytics:returns:read")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/analytics/reports/saved/**").hasAuthority("analytics:reports:read")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/analytics/reports/saved").hasAuthority("analytics:reports:read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/analytics/reports/saved").hasAuthority("analytics:reports:manage")
                        .requestMatchers(HttpMethod.PUT,  "/api/v1/analytics/reports/saved/**").hasAuthority("analytics:reports:manage")
                        .requestMatchers(HttpMethod.DELETE,"/api/v1/analytics/reports/saved/**").hasAuthority("analytics:reports:manage")
                        .requestMatchers(HttpMethod.POST, "/api/v1/analytics/reports/saved/*/run").hasAuthority("analytics:reports:run")
                        .requestMatchers(HttpMethod.POST, "/api/v1/analytics/reports/run").hasAuthority("analytics:reports:run")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/analytics/reports/scheduled/**").hasAuthority("analytics:reports:read")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/analytics/reports/scheduled").hasAuthority("analytics:reports:read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/analytics/reports/scheduled").hasAuthority("analytics:reports:manage")
                        .requestMatchers(HttpMethod.PUT,  "/api/v1/analytics/reports/scheduled/**").hasAuthority("analytics:reports:manage")
                        .requestMatchers(HttpMethod.DELETE,"/api/v1/analytics/reports/scheduled/**").hasAuthority("analytics:reports:manage")
                        .requestMatchers(HttpMethod.POST, "/api/v1/analytics/reports/scheduled/*/toggle").hasAuthority("analytics:reports:manage")
                        .requestMatchers(HttpMethod.POST, "/api/v1/analytics/reports/scheduled/*/run-now").hasAuthority("analytics:reports:run")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/analytics/reports/executions/**").hasAuthority("analytics:reports:read")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/analytics/reports/executions").hasAuthority("analytics:reports:read")

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