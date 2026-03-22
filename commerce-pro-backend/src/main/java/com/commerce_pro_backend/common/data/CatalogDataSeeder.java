package com.commerce_pro_backend.common.data;

import com.commerce_pro_backend.catalog.attribute.dto.AttributeDto;
import com.commerce_pro_backend.catalog.attribute.service.AttributeService;
import com.commerce_pro_backend.catalog.brand.dto.BrandDto;
import com.commerce_pro_backend.catalog.brand.service.BrandService;
import com.commerce_pro_backend.catalog.category.entity.Category;
import com.commerce_pro_backend.catalog.collection.dto.CollectionDto;
import com.commerce_pro_backend.catalog.collection.service.CollectionService;
import com.commerce_pro_backend.catalog.product.dto.ProductSummaryDTO;
import com.commerce_pro_backend.catalog.product.service.ProductService;
import com.commerce_pro_backend.catalog.review.dto.ReviewDto;
import com.commerce_pro_backend.catalog.review.service.ReviewService;
import com.commerce_pro_backend.catalog.seo.dto.SeoDto;
import com.commerce_pro_backend.catalog.seo.service.SeoService;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CatalogDataSeeder {

    private final SeedDataLoader seedDataLoader;

    @Bean
    @Profile("dev")
    @Order(4)
    CommandLineRunner seedCatalog(
            BrandService brandService,
            com.commerce_pro_backend.catalog.category.repository.CategoryRepository categoryRepository,
            AttributeService attributeService,
            CollectionService collectionService,
            ReviewService reviewService,
            SeoService seoService,
            ProductService productService,
            com.commerce_pro_backend.customer.repository.CustomerRepository customerRepository,
            UserDetailsService userDetailsService) {
        return args -> {
            log.info("━━━ [Seed] Catalog ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            try {
                seedDataLoader.authenticateAsSuperAdmin(userDetailsService);

                seedBrands(brandService);
                seedCategories(categoryRepository);
                seedAttributes(attributeService);

                List<ProductSummaryDTO> products = productService.getAllProducts();
                seedCollections(collectionService, products);

                var dbCustomers = customerRepository.findAll(
                        org.springframework.data.domain.PageRequest.of(0, 20)).getContent();
                seedReviews(reviewService, products, dbCustomers);
                seedSeo(seoService, products);

                log.info("[Seed] Catalog seed complete");

            } catch (Exception e) {
                log.error("[Seed] Catalog seed failed: {}", e.getMessage(), e);
            } finally {
                seedDataLoader.clearSecurityContext();
            }
        };
    }

    private void seedBrands(BrandService brandService) {
        JsonNode brandsJson = seedDataLoader.loadTree("seed/brands.json");
        int created = 0;

        for (int i = 0; i < brandsJson.size(); i++) {
            JsonNode b = brandsJson.get(i);
            try {
                brandService.createBrand(BrandDto.Request.builder()
                        .name(b.get("name").asText())
                        .slug(b.get("slug").asText())
                        .description(b.get("description").asText())
                        .logo(b.has("logo") ? b.get("logo").asText() : null)
                        .website(b.has("website") ? b.get("website").asText() : null)
                        .isActive(true)
                        .isFeatured(b.path("isFeatured").asBoolean(false))
                        .sortOrder(i + 1)
                        .build());
                created++;
            } catch (Exception e) {
                log.warn("[Seed] Brand '{}' skipped: {}", b.get("name").asText(), e.getMessage());
            }
        }
        log.info("[Seed] Created {} brands", created);
    }

    private void seedCategories(
            com.commerce_pro_backend.catalog.category.repository.CategoryRepository categoryRepository) {
        JsonNode catJson = seedDataLoader.loadTree("seed/categories.json");
        JsonNode roots = catJson.get("rootCategories");

        for (JsonNode root : roots) {
            var cat = Category.builder()
                    .name(root.get("name").asText())
                    .slug(root.get("slug").asText())
                    .description(root.get("description").asText())
                    .imageUrl(root.get("imageUrl").asText())
                    .isActive(true).showInMenu(true)
                    .sortOrder(root.get("sortOrder").asInt())
                    .seoTitle(root.get("seoTitle").asText())
                    .seoDescription(root.get("seoDescription").asText())
                    .tenantId("default").createdBy("system").updatedBy("system")
                    .hierarchyLevel(0).build();

            var saved = categoryRepository.save(cat);
            saved.setMaterializedPath("/" + saved.getId());
            categoryRepository.save(saved);

            JsonNode subs = root.path("subCategories");
            for (int i = 0; i < subs.size(); i++) {
                JsonNode sub = subs.get(i);
                var child = Category.builder()
                        .name(sub.get("name").asText())
                        .slug(sub.get("slug").asText())
                        .description(sub.get("description").asText())
                        .parent(saved).hierarchyLevel(1)
                        .isActive(true).showInMenu(true)
                        .sortOrder(i + 1)
                        .tenantId("default").createdBy("system").updatedBy("system")
                        .build();
                var savedChild = categoryRepository.save(child);
                savedChild.setMaterializedPath(saved.getMaterializedPath() + "/" + savedChild.getId());
                categoryRepository.save(savedChild);
            }
        }
        log.info("[Seed] Created {} root categories with sub-categories", roots.size());
    }

    private void seedAttributes(AttributeService attributeService) {
        JsonNode attrsJson = seedDataLoader.loadTree("seed/attributes.json");

        for (JsonNode attr : attrsJson) {
            try {
                var builder = AttributeDto.Request.builder()
                        .name(attr.get("name").asText())
                        .code(attr.get("code").asText())
                        .type(attr.get("type").asText())
                        .description(attr.get("description").asText())
                        .isRequired(attr.get("isRequired").asBoolean())
                        .isFilterable(attr.get("isFilterable").asBoolean())
                        .isSearchable(attr.get("isSearchable").asBoolean())
                        .isActive(true)
                        .sortOrder(attr.get("sortOrder").asInt());

                if (attr.has("options")) {
                    List<AttributeDto.OptionRequest> options = new ArrayList<>();
                    for (JsonNode opt : attr.get("options")) {
                        var optBuilder = AttributeDto.OptionRequest.builder()
                                .value(opt.get("value").asText())
                                .label(opt.get("label").asText())
                                .sortOrder(opt.get("sortOrder").asInt());
                        if (opt.has("colorHex")) {
                            optBuilder.colorHex(opt.get("colorHex").asText());
                        }
                        options.add(optBuilder.build());
                    }
                    builder.options(options);
                }

                attributeService.createAttribute(builder.build());
            } catch (Exception e) {
                log.warn("[Seed] Attribute '{}' skipped: {}", attr.get("name").asText(), e.getMessage());
            }
        }
        log.info("[Seed] Created {} product attributes", attrsJson.size());
    }

    private void seedCollections(CollectionService collectionService,
                                  List<ProductSummaryDTO> products) {
        JsonNode collectionsJson = seedDataLoader.loadTree("seed/collections.json");
        List<String> allProductIds = products.stream().map(ProductSummaryDTO::getId).toList();

        for (JsonNode col : collectionsJson) {
            try {
                var builder = CollectionDto.Request.builder()
                        .name(col.get("name").asText())
                        .slug(col.get("slug").asText())
                        .description(col.get("description").asText())
                        .type(col.get("type").asText())
                        .status(col.get("status").asText())
                        .sortOrder(col.get("sortOrder").asInt())
                        .isFeatured(col.get("isFeatured").asBoolean());

                if (col.has("imageUrl")) builder.imageUrl(col.get("imageUrl").asText());
                if (col.has("conditions")) builder.conditions(col.get("conditions").asText());

                // Resolve product IDs based on slice config
                List<String> productIds = resolveProductIds(col, products, allProductIds);
                builder.productIds(productIds);

                if (col.has("startDaysFromNow")) {
                    builder.startDate(Instant.now().plus(col.get("startDaysFromNow").asInt(), ChronoUnit.DAYS));
                    builder.endDate(Instant.now().plus(col.get("endDaysFromNow").asInt(), ChronoUnit.DAYS));
                }

                collectionService.createCollection(builder.build());
            } catch (Exception e) {
                log.warn("[Seed] Collection '{}' skipped: {}", col.get("name").asText(), e.getMessage());
            }
        }
        log.info("[Seed] Created {} collections", collectionsJson.size());
    }

    private List<String> resolveProductIds(JsonNode col, List<ProductSummaryDTO> products,
                                            List<String> allProductIds) {
        if (col.has("filterByMaxPrice")) {
            BigDecimal maxPrice = new BigDecimal(col.get("filterByMaxPrice").asText());
            return products.stream()
                    .filter(p -> p.getPrice().compareTo(maxPrice) < 0)
                    .map(ProductSummaryDTO::getId).toList();
        }

        JsonNode slice = col.path("productSlice");
        if (slice.isMissingNode()) return allProductIds;

        if (slice.has("fromEnd")) {
            int fromEnd = slice.get("fromEnd").asInt();
            int start = Math.max(0, allProductIds.size() - fromEnd);
            return allProductIds.subList(start, allProductIds.size());
        }

        if (slice.has("firstAndLast")) {
            int first = slice.get("firstAndLast").get("first").asInt();
            int last = slice.get("firstAndLast").get("last").asInt();
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < Math.min(first, allProductIds.size()); i++) ids.add(allProductIds.get(i));
            for (int i = Math.max(0, allProductIds.size() - last); i < allProductIds.size(); i++) {
                if (!ids.contains(allProductIds.get(i))) ids.add(allProductIds.get(i));
            }
            return ids;
        }

        int from = slice.path("from").asInt(0);
        int count = slice.path("count").asInt(allProductIds.size());
        int safeTo = Math.min(from + count, allProductIds.size());
        return allProductIds.subList(from, safeTo);
    }

    private void seedReviews(ReviewService reviewService,
                              List<ProductSummaryDTO> products,
                              List<com.commerce_pro_backend.customer.entity.Customer> dbCustomers) {
        if (dbCustomers.isEmpty() || products.isEmpty()) return;
        JsonNode reviewsJson = seedDataLoader.loadTree("seed/reviews.json");
        int created = 0;

        for (JsonNode review : reviewsJson.get("primaryReviews")) {
            created += createReview(reviewService, products, dbCustomers, review);
        }
        for (JsonNode review : reviewsJson.get("extraReviews")) {
            created += createReview(reviewService, products, dbCustomers, review);
        }
        log.info("[Seed] Created {} reviews", created);
    }

    private int createReview(ReviewService reviewService,
                              List<ProductSummaryDTO> products,
                              List<com.commerce_pro_backend.customer.entity.Customer> dbCustomers,
                              JsonNode review) {
        try {
            int pIdx = review.get("productIndex").asInt();
            int cOffset = review.get("customerOffset").asInt() % dbCustomers.size();
            if (pIdx >= products.size()) return 0;

            var product = products.get(pIdx);
            var customer = dbCustomers.get(cOffset);

            reviewService.createReview(ReviewDto.Request.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .customerName(customer.getFirstName() + " " + customer.getLastName())
                    .customerEmail(customer.getEmail())
                    .rating(review.get("rating").asInt())
                    .title(review.get("title").asText())
                    .content(review.get("content").asText())
                    .isVerifiedPurchase(review.get("verified").asBoolean())
                    .build());
            return 1;
        } catch (Exception e) {
            log.warn("[Seed] Review skipped: {}", e.getMessage());
            return 0;
        }
    }

    private void seedSeo(SeoService seoService, List<ProductSummaryDTO> products) {
        int created = 0;
        for (int i = 0; i < Math.min(10, products.size()); i++) {
            ProductSummaryDTO p = products.get(i);
            try {
                seoService.upsertSeoForEntity("PRODUCT", p.getId(), SeoDto.Request.builder()
                        .metaTitle(p.getName() + " - Buy Online | Commerce Pro")
                        .metaDescription("Shop " + p.getName() + " at the best price. Free shipping on orders over $50.")
                        .metaKeywords(p.getCategory() != null ? p.getCategory().toLowerCase() : "")
                        .canonicalUrl("/products/" + p.getSku().toLowerCase())
                        .ogTitle(p.getName())
                        .ogDescription("Get " + p.getName() + " at Commerce Pro Store")
                        .ogImage(p.getImage())
                        .robotsDirective("index, follow")
                        .slug(p.getSku().toLowerCase())
                        .build());
                created++;
            } catch (Exception e) {
                log.warn("[Seed] SEO skipped for product {}: {}", p.getName(), e.getMessage());
            }
        }
        log.info("[Seed] Created {} SEO entries", created);
    }
}
