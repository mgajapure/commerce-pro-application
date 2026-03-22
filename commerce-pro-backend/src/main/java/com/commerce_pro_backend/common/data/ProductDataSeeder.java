package com.commerce_pro_backend.common.data;

import com.commerce_pro_backend.catalog.product.dto.ProductAttributeDTO;
import com.commerce_pro_backend.catalog.product.dto.ProductRequestDTO;
import com.commerce_pro_backend.catalog.product.dto.ProductVariantDTO;
import com.commerce_pro_backend.catalog.product.service.ProductService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ProductDataSeeder {

    private final SeedDataLoader seedDataLoader;

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

            JsonNode productsJson = seedDataLoader.loadTree("seed/products.json");
            int created = 0;

            for (JsonNode node : productsJson) {
                try {
                    ProductRequestDTO req = buildProductFromJson(node);
                    productService.createProduct(req);
                    created++;
                } catch (Exception e) {
                    log.error("[Seed] Failed to create product '{}': {}",
                            node.path("name").asText(), e.getMessage());
                }
            }
            log.info("[Seed] Products complete — {} / {} created", created, productsJson.size());
        };
    }

    private ProductRequestDTO buildProductFromJson(JsonNode node) {
        BigDecimal price = new BigDecimal(node.get("price").asText());
        BigDecimal compareAtPrice = new BigDecimal(node.get("compareAtPrice").asText());
        String description = node.get("description").asText();

        List<String> tags = new ArrayList<>();
        node.path("tags").forEach(t -> tags.add(t.asText()));

        List<String> gallery = new ArrayList<>();
        node.path("gallery").forEach(g -> gallery.add(g.asText()));

        List<ProductAttributeDTO> attributes = new ArrayList<>();
        for (JsonNode attrNode : node.path("attributes")) {
            List<String> values = new ArrayList<>();
            attrNode.path("values").forEach(v -> values.add(v.asText()));
            attributes.add(ProductAttributeDTO.builder()
                    .name(attrNode.get("name").asText())
                    .values(values)
                    .displayOrder(attrNode.get("displayOrder").asInt())
                    .build());
        }

        List<ProductVariantDTO> variants = new ArrayList<>();
        for (JsonNode varNode : node.path("variants")) {
            List<String> options = new ArrayList<>();
            varNode.path("options").forEach(o -> options.add(o.asText()));
            variants.add(ProductVariantDTO.builder()
                    .name(varNode.get("name").asText())
                    .options(options)
                    .build());
        }

        return ProductRequestDTO.builder()
                .name(node.get("name").asText())
                .sku(node.get("sku").asText())
                .description(description)
                .shortDescription(description.substring(0, Math.min(100, description.length())) + "...")
                .category(node.get("category").asText())
                .brand(node.get("brand").asText())
                .price(price)
                .compareAtPrice(compareAtPrice)
                .cost(price.multiply(new BigDecimal("0.60")))
                .stock(node.get("stock").asInt())
                .lowStockThreshold(10)
                .status("active")
                .visibility("visible")
                .image(node.get("image").asText())
                .gallery(gallery)
                .tags(tags)
                .featured(Math.random() > 0.70)
                .trackInventory(true)
                .allowBackorders(false)
                .vendor(node.get("brand").asText())
                .productType("Physical")
                .attributes(attributes)
                .variants(variants)
                .build();
    }
}
