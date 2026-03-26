package com.commerce_pro_backend.common.data;

import com.commerce_pro_backend.supplier.directory.dto.SupplierDto;
import com.commerce_pro_backend.supplier.directory.service.SupplierService;
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

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SupplierDataSeeder {

    private final SeedDataLoader seedDataLoader;

    @Bean
    @Profile("dev")
    @Order(11)
    CommandLineRunner seedSuppliers(
            SupplierService supplierService,
            UserDetailsService userDetailsService) {
        return args -> {
            log.info("━━━ [Seed] Suppliers ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            try {
                seedDataLoader.authenticateAsSuperAdmin(userDetailsService);
                seedSupplierDirectory(supplierService);
                log.info("[Seed] Supplier seed complete");
            } catch (Exception e) {
                log.error("[Seed] Supplier seed failed: {}", e.getMessage(), e);
            } finally {
                seedDataLoader.clearSecurityContext();
            }
        };
    }

    private void seedSupplierDirectory(SupplierService supplierService) {
        JsonNode suppliersJson = seedDataLoader.loadTree("seed/suppliers.json");
        int created = 0;

        for (int i = 0; i < suppliersJson.size(); i++) {
            JsonNode s = suppliersJson.get(i);
            try {
                supplierService.createSupplier(SupplierDto.Request.builder()
                        .name(s.get("name").asText())
                        .code(s.get("code").asText())
                        .contactPerson(s.path("contactPerson").asText(null))
                        .email(s.path("email").asText(null))
                        .phone(s.path("phone").asText(null))
                        .address(s.path("address").asText(null))
                        .city(s.path("city").asText(null))
                        .state(s.path("state").asText(null))
                        .country(s.path("country").asText(null))
                        .postalCode(s.path("postalCode").asText(null))
                        .website(s.path("website").asText(null))
                        .description(s.path("description").asText(null))
                        .paymentTerms(s.path("paymentTerms").asText(null))
                        .paymentTermsDays(s.path("paymentTermsDays").asInt(30))
                        .leadTimeDays(s.path("leadTimeDays").asInt(14))
                        .minOrderValue(s.has("minOrderValue")
                                ? new BigDecimal(s.get("minOrderValue").asText())
                                : null)
                        .preferredCurrency(s.path("preferredCurrency").asText("USD"))
                        .certifications(s.path("certifications").asText(null))
                        .notes(s.path("notes").asText(null))
                        .isActive(true)
                        .isPreferred(s.path("isPreferred").asBoolean(false))
                        .build());
                created++;
            } catch (Exception e) {
                log.warn("[Seed] Supplier '{}' skipped: {}", s.get("name").asText(), e.getMessage());
            }
        }
        log.info("[Seed] Created {} suppliers", created);
    }
}
