package com.commerce_pro_backend.common.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
public class SeedDataLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public <T> List<T> loadList(String resourcePath, TypeReference<List<T>> typeRef) {
        try (InputStream is = new ClassPathResource(resourcePath).getInputStream()) {
            return MAPPER.readValue(is, typeRef);
        } catch (IOException e) {
            log.error("[Seed] Failed to load {}: {}", resourcePath, e.getMessage());
            return List.of();
        }
    }

    public JsonNode loadTree(String resourcePath) {
        try (InputStream is = new ClassPathResource(resourcePath).getInputStream()) {
            return MAPPER.readTree(is);
        } catch (IOException e) {
            log.error("[Seed] Failed to load {}: {}", resourcePath, e.getMessage());
            return MAPPER.createObjectNode();
        }
    }

    public ObjectMapper mapper() {
        return MAPPER;
    }

    public void authenticateAsSuperAdmin(UserDetailsService userDetailsService) {
        var userDetails = userDetailsService.loadUserByUsername("superadmin");
        var auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
