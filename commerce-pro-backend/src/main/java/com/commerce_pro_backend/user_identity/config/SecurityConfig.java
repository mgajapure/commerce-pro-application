package com.commerce_pro_backend.user_identity.config;

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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
                        .ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**"))
                        .disable())
                // Configure headers before authorizeHttpRequests
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                        .contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors 'self'")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - H2 Console must be first
                        .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
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
                        .requestMatchers(HttpMethod.PATCH,  "/api/v1/orders/**/tracking").hasAuthority("order:order:update")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/orders/**").hasAuthority("order:order:cancel")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/orders/**/confirm").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/orders/**/process").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/orders/**/ship").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/orders/**/deliver").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/orders/**/hold").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/orders/**/release-hold").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/orders/**/close").hasAuthority("order:order:manage-status")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/orders/bulk-action").hasAuthority("order:order:bulk-action")

                        // All other requests need authentication
                        .anyRequest().authenticated())
                .addFilterBefore(superAdminFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

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
