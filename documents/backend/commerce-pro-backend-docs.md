# Commerce Pro Backend — Complete Technical Documentation

> **Generated from source** · Spring Boot 3.5.11 / Java 17 / Gradle 8.14.4  
> Base API URL: `http://localhost:8080/api` · H2 Console: `http://localhost:8080/api/h2-console` · Swagger UI: `http://localhost:8080/api/swagger-ui/index.html`

---

## Table of Contents

1. [Project Overview & Stack](#1-project-overview--stack)
2. [Build & Configuration](#2-build--configuration)
3. [Common / Shared Module](#3-common--shared-module)
4. [Catalog — Brand Module](#4-catalog--brand-module)
5. [Catalog — Category Module](#5-catalog--category-module)
6. [Catalog — Product Module](#6-catalog--product-module)
7. [Inventory Module](#7-inventory-module)
8. [Order Module](#8-order-module)
9. [Fulfillment Module](#9-fulfillment-module)
10. [User Identity Module](#10-user-identity-module)
11. [Security Architecture](#11-security-architecture)
12. [Data Initializer](#12-data-initializer)
13. [Cross-Module Integration Map](#13-cross-module-integration-map)
14. [Architectural Rules Reference](#14-architectural-rules-reference)

---

## 1. Project Overview & Stack

Commerce Pro is an enterprise-grade e-commerce business management platform. The backend is a Spring Boot monolith with the following technology stack:

| Component | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.11 |
| Build tool | Gradle 8.14.4 (Wrapper) |
| Database (dev) | H2 File-based (`./data/commerce-pro-db`) |
| Database (prod) | MySQL / PostgreSQL (driver commented in `build.gradle`) |
| ORM | Spring Data JPA / Hibernate 6 |
| Security | Spring Security + JWT (jjwt 0.12.6, HS512) |
| API docs | SpringDoc OpenAPI 2.8.5 (Swagger UI) |
| DTO mapping | Manual mappers + MapStruct 1.6.3 (on classpath) |
| Code generation | Lombok |
| Scheduling | Spring `@EnableScheduling` |

### Package Structure

```
com.commerce_pro_backend/
├── common/            # Shared cross-cutting concerns
│   ├── config/        # CorsConfig, WebConfig
│   ├── converter/     # CustomFieldsConverter (JPA AttributeConverter)
│   ├── data/          # DataInitializer (@Profile("dev"))
│   ├── dto/           # ApiResponse<T>, PageResponse<T>
│   ├── exception/     # ApiException, GlobalExceptionHandler
│   └── storage/       # FileStorageService, FileStorageController
├── catalog/           # Product catalog module
│   ├── brand/
│   ├── category/
│   └── product/
├── inventory/         # Inventory & warehouse module
├── order/             # Order management module
├── fulfillment/       # Fulfillment, WMS, shipment module
└── user_identity/     # Auth, RBAC, audit module
```

---

## 2. Build & Configuration

### `build.gradle` (Key Dependencies)

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.11'
    id 'io.spring.dependency-management' version '1.1.7'
}

java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }

dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-security'

    // OpenAPI/Swagger
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // MapStruct
    implementation 'org.mapstruct:mapstruct:1.6.3'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.6.3'

    // Apache Commons
    implementation 'org.apache.commons:commons-lang3:3.17.0'
    implementation 'commons-io:commons-io:2.18.0'
    implementation 'org.apache.commons:commons-collections4:4.4'
    implementation 'commons-codec:commons-codec:1.17.1'     // Base32 for TOTP

    // JSON
    implementation 'com.google.code.gson:gson:2.12.1'

    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly    'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly    'io.jsonwebtoken:jjwt-jackson:0.12.6'

    // Database
    runtimeOnly 'com.h2database:h2'
    // runtimeOnly 'com.mysql:mysql-connector-j:9.2.0'     // Prod
    // runtimeOnly 'org.postgresql:postgresql:42.7.5'       // Prod
}

// Run command activates the 'dev' Spring profile automatically
tasks.named('bootRun') {
    jvmArgs = ['-Dspring.profiles.active=dev']
}

// MapStruct compiler options
tasks.withType(JavaCompile) {
    options.compilerArgs = [
        '-parameters',
        '-Amapstruct.defaultComponentModel=spring',
        '-Amapstruct.unmappedTargetPolicy=IGNORE'
    ]
}

tasks.wrapper { gradleVersion = '8.14.4' }
```

### `application.properties`

```properties
server.port=8080
server.servlet.context-path=/api          # All endpoints are under /api

# H2 Database (dev)
spring.datasource.url=jdbc:h2:file:./data/commerce-pro-db
spring.datasource.username=admin
spring.datasource.password=admin
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop    # Schema rebuilt fresh every restart
spring.jpa.show-sql=false

# HikariCP
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000

# Jackson
spring.jackson.serialization.write-dates-as-timestamps=false
spring.jackson.date-format=yyyy-MM-dd HH:mm:ss
spring.jackson.time-zone=UTC

# File Upload
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB
app.file.upload-dir=./uploads

# CORS
cors.allowed-origins=http://localhost:4200,http://127.0.0.1:4200,http://localhost:3000

# Pagination
spring.data.web.pageable.default-page-size=20
spring.data.web.pageable.max-page-size=100

spring.profiles.active=dev
```

### `application-identity.properties`

```properties
# SuperAdmin credentials
commercepro.superadmin.default-credentials.username=superadmin
commercepro.superadmin.default-credentials.email=superadmin@commercepro.local
commercepro.superadmin.default-credentials.password=${SUPERADMIN_PASSWORD:superadmin}
commercepro.superadmin.default-credentials.force-password-change-on-first-login=true

# Security policy
commercepro.superadmin.security-policy.require-mfa=false
commercepro.superadmin.security-policy.session-timeout-minutes=30
commercepro.superadmin.security-policy.max-concurrent-sessions=3

# JWT
app.jwt.secret=${JWT_SECRET:1c4a63a63daeeb9c542df5da3ea6cecf59108ff67c289488ed118c4f1a2501e7}
app.jwt.expiration-ms=900000           # 15 minutes
app.jwt.refresh-expiration-ms=604800000  # 7 days

# Role hierarchy
commercepro.roles.hierarchy-rules.max-depth=5
commercepro.roles.hierarchy-rules.allow-multiple-parents=true
commercepro.roles.hierarchy-rules.enforce-acyclic=true
```

### Main Application Class

```java
@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
public class CommerceProBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommerceProBackendApplication.class, args);
    }
}
```

---

## 3. Common / Shared Module

### 3.1 `ApiResponse<T>` — Standard Response Wrapper

All endpoints return `ResponseEntity<ApiResponse<T>>`.

```java
@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Object error;
    private long timestamp;
    private String path;

    // Factory methods
    public static <T> ApiResponse<T> success(T data) { ... }
    public static <T> ApiResponse<T> success(String message, T data) { ... }
    public static <T> ApiResponse<T> error(String message) { ... }
    public static <T> ApiResponse<T> error(String message, Object errorDetails) { ... }
}
```

**JSON shape (success):**
```json
{
  "success": true,
  "message": "Product retrieved successfully",
  "data": { ... },
  "timestamp": 1710000000000
}
```

**JSON shape (error):**
```json
{
  "success": false,
  "message": "Product not found with id: abc",
  "error": { "code": "RESOURCE_NOT_FOUND" },
  "timestamp": 1710000000000,
  "path": "/api/products/abc"
}
```

### 3.2 `PageResponse<T>` — Paginated Response Wrapper

```java
@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;

    // From Spring Data Page
    public static <T> PageResponse<T> from(Page<T> page) { ... }
    public static <E, D> PageResponse<D> from(Page<E> page, List<D> dtoContent) { ... }
}
```

### 3.3 `ApiException` — Unified Error Handling

```java
@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    // Factory methods — always use these, never throw raw RuntimeException
    public static ApiException notFound(String resource, String id)  // 404
    public static ApiException badRequest(String message)            // 400
    public static ApiException conflict(String message)              // 409
    public static ApiException unauthorized(String message)          // 401
    public static ApiException forbidden(String message)             // 403
}
```

### 3.4 `GlobalExceptionHandler`

Handles all exceptions and maps them to `ApiResponse` shapes:

| Exception | HTTP Status | Behaviour |
|---|---|---|
| `ApiException` | From `.status` field | Returns `ApiResponse.error` with `errorCode` |
| `MethodArgumentNotValidException` | 400 | Returns field → message map in `error` field |
| `ConstraintViolationException` | 400 | Returns `ApiResponse.error` |
| `AuthenticationException` | 401 | Returns `UNAUTHORIZED` code |
| `AccessDeniedException` | 403 | Returns `FORBIDDEN` code |
| `Exception` | 500 | Returns `INTERNAL_SERVER_ERROR` |

### 3.5 `CustomFieldsConverter`

JPA `AttributeConverter<Map<String, String>, String>` that serialises/deserialises `Map<String, String>` to/from a JSON column in the database.

```java
@Converter
public class CustomFieldsConverter
        implements AttributeConverter<Map<String, String>, String> {
    // Converts Map → JSON string for DB storage
    // Converts JSON string → Map for Java use
    // Handles null and blank gracefully
}
```

### 3.6 `CorsConfig`

Reads `cors.allowed-origins` (comma-separated) from `application.properties` and creates a `CorsFilter` bean allowing `GET, POST, PUT, PATCH, DELETE, OPTIONS` with credentials.

### 3.7 `WebConfig`

Configures `PageableHandlerMethodArgumentResolver` with:
- 0-based page numbering
- Maximum page size: 100

### 3.8 `FileStorageService`

Manages file upload/download at `./uploads/`.

**Subdirectories created at startup:** `products/`, `categories/`, `brands/`

```java
@Service
public class FileStorageService {
    // storeFile(MultipartFile, subdirectory) → returns relative path e.g. "products/uuid.jpg"
    // storeProductImage(MultipartFile) → convenience wrapper for "products/" subdirectory
    // loadFileAsResource(fileName) → Resource for download
    // deleteFile(fileName)
    // validateImageFile(MultipartFile) → checks content type + 10 MB limit
}
```

### 3.9 `FileStorageController`

Base path: `/files`

| Method | Path | Description |
|---|---|---|
| POST | `/upload/product` | Upload product image (validates image type) |
| POST | `/upload` | Upload generic file with optional `subdirectory` param |
| GET | `/download/{fileName:.+}` | Download file (Content-Disposition: attachment) |
| GET | `/view/{fileName:.+}` | View file inline (for images) |
| DELETE | `/{fileName:.+}` | Delete file |

**File URL pattern:** `/api/files/download/<subdirectory>/<uuid>.<ext>`

---

## 4. Catalog — Brand Module

**Package:** `com.commerce_pro_backend.catalog.brand`  
**Controller base path:** `/api/v1/brands`

### 4.1 Entity: `Brand`

**Table:** `brands`

| Column | Type | Constraints |
|---|---|---|
| `id` | VARCHAR(36) | PK, UUID (set manually) |
| `name` | VARCHAR(100) | NOT NULL |
| `slug` | VARCHAR(150) | NOT NULL, UNIQUE |
| `description` | VARCHAR(2000) | nullable |
| `logo_url` | VARCHAR(500) | nullable |
| `website` | VARCHAR(500) | nullable, URL-validated |
| `is_active` | BOOLEAN | NOT NULL, default `true` |
| `is_featured` | BOOLEAN | NOT NULL, default `false` |
| `product_count` | INTEGER | NOT NULL, default `0` |
| `sort_order` | INTEGER | NOT NULL, default `0` |
| `created_at` | TIMESTAMP | auto, not updatable |
| `updated_at` | TIMESTAMP | auto-updated |
| `version` | BIGINT | optimistic locking |

**Indexes:** `slug` (unique), `is_active`, `is_featured`, `sort_order`

**Business methods on entity:**
- `activate()` / `deactivate()` — set `isActive`
- `feature()` / `unfeature()` — set `isFeatured`
- `incrementProductCount()` / `decrementProductCount()` / `updateProductCount(int)`
- `@PrePersist @PreUpdate normalize()` — lowercases slug, prepends `https://` to website if missing

```java
@Entity @Table(name = "brands")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand {
    @Id @Column(length = 36)
    private String id;

    @NotBlank @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank @Size(max = 150)
    @Column(nullable = false, length = 150, unique = true)
    private String slug;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logo;

    @Column(length = 500)
    private String website;

    @Column(name = "is_active", nullable = false) @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_featured", nullable = false) @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "product_count", nullable = false) @Builder.Default
    private Integer productCount = 0;

    @Column(name = "sort_order", nullable = false) @Builder.Default
    private Integer sortOrder = 0;

    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;
    // ... business methods
}
```

### 4.2 DTOs: `BrandDto`

A single file with three static inner classes:

```java
public class BrandDto {
    // Request — for create/update
    @Data @Builder
    public static class Request {
        @NotBlank @Size(max = 100) String name;
        @NotBlank @Size(max = 150)
        @Pattern(regexp = "^[a-z0-9-]+$") String slug;
        @Size(max = 2000) String description;
        @Size(max = 500) String logo;
        String website;
        Boolean isActive;
        Boolean isFeatured;
        Integer sortOrder;
    }

    // Response — full detail
    @Data @Builder
    public static class Response {
        String id, name, slug, description, logo, website;
        Boolean isActive, isFeatured;
        Integer productCount, sortOrder;
        Instant createdAt, updatedAt;
    }

    // ListResponse — lightweight for list views
    @Data @Builder
    public static class ListResponse {
        String id, name, slug, logo;
        Boolean isActive, isFeatured;
        Integer productCount;
    }
}
```

### 4.3 Repository: `BrandRepository`

```java
public interface BrandRepository extends JpaRepository<Brand, String>,
        JpaSpecificationExecutor<Brand> {

    Optional<Brand> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Brand> findByIsActiveTrueOrderBySortOrderAsc();
    List<Brand> findByIsFeaturedTrueAndIsActiveTrueOrderBySortOrderAsc();
    Page<Brand> findByIsActiveTrueOrderBySortOrderAsc(Pageable pageable);

    @Modifying @Query("UPDATE Brand b SET b.productCount = b.productCount + :delta WHERE b.id = :id")
    int updateProductCount(String id, int delta);

    @Modifying @Query("UPDATE Brand b SET b.isActive = :active WHERE b.id = :id")
    int updateActiveStatus(String id, boolean active);

    @Modifying @Query("UPDATE Brand b SET b.isFeatured = :featured WHERE b.id = :id")
    int updateFeaturedStatus(String id, boolean featured);
}
```

### 4.4 Mapper: `BrandMapper`

Manual mapper (`@Component`). Methods: `toEntity(Request)`, `toResponse(Brand)`, `toListResponse(Brand)`, `toListResponseList(List<Brand>)`, `updateEntityFromDto(Request, Brand)`.

### 4.5 Service: `BrandService`

```java
@Service @RequiredArgsConstructor @Slf4j
@Transactional(readOnly = true)
public class BrandService {
    @Cacheable("brands")  getAllActiveBrands() → List<ListResponse>
    @Cacheable("brands")  getFeaturedBrands() → List<ListResponse>
                          getAllBrands(Pageable) → Page<ListResponse>
                          getBrand(id) → Response
                          getBrandBySlug(slug) → Response
    @Transactional @CacheEvict  createBrand(Request) → Response
    @Transactional @CacheEvict  updateBrand(id, Request) → Response
    @Transactional @CacheEvict  deleteBrand(id)
    @Transactional              updateProductCount(id, delta)
    @Transactional @CacheEvict  toggleActive(id, boolean)
    @Transactional @CacheEvict  toggleFeatured(id, boolean)
}
```

Note: The service uses caching annotations but the actual cache provider is not currently registered (would require `spring-boot-starter-cache`).

### 4.6 Controller: `BrandController`

| Method | Path | Description | Response |
|---|---|---|---|
| GET | `/api/v1/brands` | Paginated list | `Page<ListResponse>` |
| GET | `/api/v1/brands/all` | All active brands | `List<ListResponse>` |
| GET | `/api/v1/brands/featured` | Featured active brands | `List<ListResponse>` |
| GET | `/api/v1/brands/{id}` | Brand by ID | `Response` |
| GET | `/api/v1/brands/slug/{slug}` | Brand by slug | `Response` |
| POST | `/api/v1/brands` | Create brand | `Response` (201) |
| PUT | `/api/v1/brands/{id}` | Update brand | `Response` |
| DELETE | `/api/v1/brands/{id}` | Delete brand | 204 No Content |
| PATCH | `/api/v1/brands/{id}/active?active=true` | Toggle active | 200 |
| PATCH | `/api/v1/brands/{id}/featured?featured=true` | Toggle featured | 200 |

---

## 5. Catalog — Category Module

**Package:** `com.commerce_pro_backend.catalog.category`  
**Controller base path:** `/v1/categories` (note: no `/api` prefix in `@RequestMapping` — resolved by `server.servlet.context-path=/api`)

### 5.1 Entity: `Category`

**Table:** `categories`

Features: hierarchical tree (parent/child), materialized path, soft delete, multi-tenancy (`tenantId`), SEO fields, custom JSON fields, external integration reference.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID (generated) | PK |
| `name` | VARCHAR(100) | NOT NULL |
| `slug` | VARCHAR(150) | NOT NULL, UNIQUE |
| `description` | TEXT | nullable |
| `parent_id` | FK → `categories.id` | nullable (root categories) |
| `hierarchy_level` | INTEGER | auto-computed (0 = root) |
| `materialized_path` | VARCHAR(500) | e.g. `/rootId/childId` |
| `image_url` | VARCHAR(500) | nullable |
| `is_active` | BOOLEAN | default `true` |
| `sort_order` | INTEGER | default `0` |
| `show_in_menu` | BOOLEAN | default `true` |
| `seo_title` | VARCHAR(70) | nullable |
| `seo_description` | VARCHAR(160) | nullable |
| `meta_keywords` | VARCHAR(500) | nullable |
| `custom_fields` | JSON | stored via `CustomFieldsConverter` |
| `created_at` / `updated_at` | TIMESTAMP | auto |
| `created_by` / `updated_by` | VARCHAR(36) | user ID |
| `is_deleted` | BOOLEAN | soft delete, default `false` |
| `deleted_at` / `deleted_by` | TIMESTAMP / VARCHAR | soft delete audit |
| `tenant_id` | VARCHAR(36) | multi-tenancy |
| `external_ref` | VARCHAR(100) | integration reference |

**Relationships:**
- `@ManyToOne parent` — self-referential parent category
- `@OneToMany subcategories` — children (cascade ALL, ordered by `sortOrder`)

### 5.2 DTOs: `CategoryDto`

```java
public class CategoryDto {
    public static class Request { name, slug, description, parentId, imageUrl,
        isActive, sortOrder, showInMenu, seoTitle, seoDescription, metaKeywords,
        customFields, hierarchyLevel, materializedPath }

    public static class Response { /* full — all entity fields + subcategories */ }
    public static class ChildResponse { id, name, slug, imageUrl, hasChildren, hierarchyLevel }
    public static class TreeResponse { id, name, slug, imageUrl, hierarchyLevel, List<TreeResponse> children }
    public static class ListResponse { List<Response> categories; Long total; Integer page; Integer size }
    public static class CustomFieldUpdateRequest { Map<String, Object> fields }
    public static class MoveRequest { String newParentId }
    public static class ReorderRequest { Integer newSortOrder }
    public static class BreadcrumbResponse { id, name, slug, hierarchyLevel }
    public static class StatisticsResponse { totalCategories, categoriesByLevel, rootCategories, ... }
}
```

### 5.3 Repository: `CategoryRepository`

Rich query support:

```java
public interface CategoryRepository extends JpaRepository<Category, String>,
        JpaSpecificationExecutor<Category> {

    // Basic lookups
    Optional<Category> findBySlug(String slug);
    boolean existsBySlugAndTenantId(String slug, String tenantId);

    // Hierarchy queries
    @EntityGraph List<Category> findRootCategories(String tenantId);
    @EntityGraph List<Category> findSubcategories(String parentId);
    List<Category> findSubtree(String materializedPath);
    List<Category> findByHierarchyLevel(Integer level, String tenantId);

    // Menu/display
    List<Category> findMenuCategories(String tenantId);
    Page<Category> findAllActive(String tenantId, Pageable pageable);

    // Soft delete
    @Modifying int softDelete(String id, Instant deletedAt, String deletedBy);
    @Modifying int restore(String id);
    @Modifying int updateActiveStatus(String id, boolean active, Instant, String updatedBy);

    // Hierarchy maintenance (native SQL)
    @Modifying int updateSubtreePaths(String oldPrefix, String newPrefix, int levelDelta);

    // Entity graphs
    Optional<Category> findByIdWithParent(String id);
    Optional<Category> findByIdWithSubcategories(String id);
    Optional<Category> findByIdWithFullHierarchy(String id);

    // Recursive CTE queries (native SQL — H2 compatible)
    List<String> findAllDescendantIds(String categoryId);
    List<Object[]> findAncestorPath(String categoryId);
    long countDescendants(String materializedPath, String excludeId);

    // Bulk / stats
    @Modifying int shiftSortOrder(String parentId, int startOrder, int delta);
    long countActive(String tenantId);
    List<Object[]> countByHierarchyLevel(String tenantId);
}
```

### 5.4 Service: `CategoryService`

Key operations:

- `getCategoryTree(tenantId)` — returns recursive `TreeResponse` list (cached)
- `createCategory(Request, userId, tenantId)` — sets hierarchy level and materialized path; validates circular refs and same-tenant parent
- `updateCategory(id, Request, userId)` — handles parent change and cascades path/level updates to all descendants
- `deleteCategory(id, userId)` — soft delete; rejects if subcategories exist
- `restoreCategory(id, userId)` — un-soft-deletes
- `moveCategory(id, newParentId, userId)` — re-parents; bulk-updates descendant materialized paths
- `reorderCategory(id, newSortOrder, userId)` — shifts siblings
- `updateCustomFields(id, fields, userId)` — replaces all custom fields

### 5.5 Controller: `CategoryController`

| Method | Path | Description |
|---|---|---|
| GET | `/v1/categories/tree?tenantId=` | Full tree response |
| GET | `/v1/categories/menu?tenantId=` | Menu categories |
| GET | `/v1/categories/level/{level}?tenantId=` | By hierarchy level |
| GET | `/v1/categories` | Paginated list |
| GET | `/v1/categories/deleted` | Soft-deleted categories |
| GET | `/v1/categories/{id}` | By ID |
| GET | `/v1/categories/slug/{slug}?tenantId=` | By slug |
| POST | `/v1/categories` | Create |
| PUT | `/v1/categories/{id}` | Update |
| DELETE | `/v1/categories/{id}` | Soft delete |
| POST | `/v1/categories/{id}/restore` | Restore |
| GET | `/v1/categories/{id}/subcategories` | Direct children |
| POST | `/v1/categories/{id}/move` | Re-parent |
| POST | `/v1/categories/{id}/reorder` | Change sort order |
| GET | `/v1/categories/{id}/path` | Ancestors to root |
| GET | `/v1/categories/{id}/subtree` | Full subtree |
| GET | `/v1/categories/{id}/descendants` | Descendant IDs |
| GET | `/v1/categories/{id}/descendants/count` | Descendant count |
| GET | `/v1/categories/{id}/custom-fields` | Custom fields map |
| PUT | `/v1/categories/{id}/custom-fields` | Replace custom fields |
| PATCH | `/v1/categories/{id}/active?active=` | Toggle active |
| GET | `/v1/categories/statistics?tenantId=` | Counts by level |

---

## 6. Catalog — Product Module

**Package:** `com.commerce_pro_backend.catalog.product`  
**Controller base path:** `/products`

### 6.1 Entities

#### `Product`

**Table:** `products`

```java
@Entity @Table(name = "products")
public class Product {
    @Id @UuidGenerator String id;

    @NotBlank String name;                    // VARCHAR(255), NOT NULL
    @NotBlank @Column(unique=true) String sku; // VARCHAR(100), UNIQUE
    @Column(length=5000) String description;
    @Column(length=500) String shortDescription;
    @NotBlank String category;                // VARCHAR(100), NOT NULL
    String categoryId;                        // FK to categories table (optional)
    @NotBlank String brand;                   // VARCHAR(100), NOT NULL

    @Column(precision=19, scale=4) BigDecimal price;         // NOT NULL
    @Column(precision=19, scale=4) BigDecimal compareAtPrice;
    @Column(precision=19, scale=4) BigDecimal cost;

    Integer stock;                            // NOT NULL, >= 0
    Integer lowStockThreshold;                // NOT NULL, >= 0
    String stockStatus;                       // "in_stock" | "low_stock" | "out_of_stock"
    String status;                            // "active" | "draft" | "archived" | ...
    String visibility;                        // "visible" | "hidden"

    @Column(columnDefinition="TEXT") String imageUrl;
    @ElementCollection List<String> gallery;       // table: product_gallery
    String featuredImage;
    BigDecimal weight;
    @Embedded Dimensions dimensions;              // length, width, height
    @ElementCollection List<String> tags;         // table: product_tags

    Boolean featured;
    Boolean trackInventory;
    Boolean allowBackorders;
    Boolean hasOrders;                        // true if any order references this product

    @Column(precision=3, scale=2) BigDecimal rating;     // 0.00 – 5.00
    Integer reviewCount;
    Integer salesCount;
    @Column(precision=19, scale=4) BigDecimal revenue;

    @OneToMany(mappedBy="product", cascade=ALL, orphanRemoval=true)
    List<ProductVariant> variants;

    @OneToMany(mappedBy="product", cascade=ALL, orphanRemoval=true)
    List<ProductAttribute> attributes;

    String vendor;
    String productType;                       // "Physical" | "Digital"
    String barcode;
    String urlHandle;
    String seoTitle;
    String seoDescription;
    String imageAlt;

    @CreationTimestamp LocalDateTime createdAt;
    @UpdateTimestamp  LocalDateTime updatedAt;

    // Business methods
    public void updateStockStatus()  // sets stockStatus based on stock vs lowStockThreshold
    @PrePersist @PreUpdate prePersistUpdate()  // calls updateStockStatus()
}
```

**Indexes:** `status`, `category`, `brand`, `sku` (unique)

#### `ProductVariant`

**Table:** `product_variants`

| Field | Notes |
|---|---|
| `id` | `@UuidGenerator` |
| `name` | VARCHAR(255), NOT NULL |
| `options` | `@ElementCollection` → table `variant_options` |
| `product` | `@ManyToOne` FK |

#### `ProductAttribute`

**Table:** `product_attributes`

| Field | Notes |
|---|---|
| `id` | `@UuidGenerator` |
| `name` | VARCHAR(100), NOT NULL |
| `values` | `@ElementCollection` → table `product_attribute_values` |
| `product` | `@ManyToOne` FK |
| `displayOrder` | INTEGER, default 0 |

#### `Dimensions` (Embeddable)

```java
@Embeddable
public class Dimensions {
    BigDecimal length;
    BigDecimal width;
    BigDecimal height;
}
```

### 6.2 DTOs

| DTO | Purpose |
|---|---|
| `ProductRequestDTO` | Create / update — full validated request |
| `ProductResponseDTO` | Full response — all fields + aliases (`quantity` = `stock`, `reviews` = `reviewCount`, etc.) |
| `ProductSummaryDTO` | List view — id, name, sku, category, brand, price, stock, image, status, hasOrders |
| `ProductDashboardDTO` | Dashboard — id, name, category, price, sold, revenue, stock, stockStatus, image |
| `ProductStatsDTO` | Aggregate stats — total, active, lowStock, outOfStock, drafts, revenue, statusCounts |
| `ProductFilterDTO` | Filter params — searchQuery, status, category, stockStatus, brand, minPrice, maxPrice, minRating, featured, sortBy, sortDirection |
| `ProductVariantDTO` | Variant — id, name, options |
| `ProductAttributeDTO` | Attribute — id, name, values, displayOrder |
| `DimensionsDTO` | Embedded — length, width, height |
| `StockUpdateDTO` | Stock adjustment — quantity, reason, adjust (true = relative, false = absolute) |

**`ProductRequestDTO` validation:**
```java
@NotBlank name, sku, category, brand, status, visibility
@NotNull price, stock, lowStockThreshold, featured, trackInventory, allowBackorders
@DecimalMin("0.0") price, compareAtPrice, cost, weight
@Pattern("active|draft|archived|out_of_stock|discontinued") status
@Pattern("visible|hidden") visibility
@Pattern("Physical|Digital") productType
@Valid dimensions, variants[], attributes[]
```

### 6.3 Repository: `ProductRepository`

```java
public interface ProductRepository extends JpaRepository<Product, String>,
        JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, String id);
    Optional<Product> findByUrlHandle(String urlHandle);

    List<Product> findByStatus(String status);
    List<Product> findByStatusAndVisibility(String status, String visibility);
    List<Product> findByCategory(String category);
    Page<Product> findByCategory(String category, Pageable pageable);
    List<Product> findByFeaturedTrue();
    List<Product> findByStockStatus(String stockStatus);

    @Query("SELECT p FROM Product p WHERE p.stock <= p.lowStockThreshold AND p.stock > 0")
    List<Product> findLowStockProducts();

    @Query("SELECT p FROM Product p WHERE p.stockStatus = 'out_of_stock'")
    List<Product> findOutOfStockProducts();

    @Query("... LIKE ...") Page<Product> search(String query, Pageable pageable);

    @Query("SELECT p FROM Product p ORDER BY p.salesCount DESC")
    List<Product> findTopSelling(Pageable pageable);

    @Query("SELECT p FROM Product p ORDER BY p.revenue DESC")
    List<Product> findTopRevenue(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = :status")
    long countByStatus(String status);

    @Query("SELECT p.status, COUNT(p) FROM Product p GROUP BY p.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT SUM(p.revenue) FROM Product p") BigDecimal sumRevenue();

    @Query("SELECT DISTINCT p.brand FROM Product p ORDER BY p.brand")
    List<String> findAllBrands();

    @Query("SELECT DISTINCT t FROM Product p JOIN p.tags t ORDER BY t")
    List<String> findAllTags();
}
```

### 6.4 `ProductSpecification`

Static factory methods returning `Specification<Product>`:

```java
public class ProductSpecification {
    static Specification<Product> withFilter(ProductFilterDTO filter)   // combined filter
    static Specification<Product> search(String query)                  // name, sku, brand LIKE
    static Specification<Product> hasStatus(String status)
    static Specification<Product> hasCategory(String category)
    static Specification<Product> hasStockStatus(String stockStatus)
    static Specification<Product> hasBrand(String brand)
    static Specification<Product> hasVisibility(String visibility)
    static Specification<Product> isFeatured(boolean featured)
    static Specification<Product> priceBetween(BigDecimal min, BigDecimal max)
    static Specification<Product> hasMinRating(int minRating)
    static Specification<Product> hasTag(String tag)
    static Specification<Product> hasVariantName(String variantName)   // joins variants
    static Specification<Product> forStorefront()                       // active + visible
    static Specification<Product> isLowStock()
    static Specification<Product> isOutOfStock()
}
```

### 6.5 Mapper: `ProductMapper`

Manual mapper with merge strategy for variants and attributes:

```java
@Component
public class ProductMapper {
    Product toEntity(ProductRequestDTO dto)
    void updateEntityFromDTO(Product product, ProductRequestDTO dto)   // merge strategy
    ProductResponseDTO toResponseDTO(Product product)
    ProductSummaryDTO toSummaryDTO(Product product)
    ProductDashboardDTO toDashboardDTO(Product product)
    List<ProductResponseDTO> toResponseDTOList(List<Product>)
    // ... list variants
}
```

**Merge strategy (variants & attributes):** Clears existing list and re-adds; `orphanRemoval=true` handles deletions. IDs with existing records are updated in place; records without ID create new ones.

### 6.6 Service: `ProductService`

```java
@Slf4j @Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    // CRUD
    ProductResponseDTO getProductById(String id)
    ProductResponseDTO getProductBySku(String sku)
    @Transactional ProductResponseDTO createProduct(ProductRequestDTO dto)
    @Transactional ProductResponseDTO updateProduct(String id, ProductRequestDTO dto)
    @Transactional void deleteProduct(String id)                 // rejects if hasOrders=true
    @Transactional ProductResponseDTO patchProduct(String id, Map<String,Object> updates)

    // Lists
    Page<ProductSummaryDTO> getProducts(ProductFilterDTO filter, Pageable pageable)
    List<ProductSummaryDTO> getAllProducts()
    Page<ProductSummaryDTO> searchProducts(String query, Pageable pageable)
    Page<ProductSummaryDTO> getProductsByCategory(String category, Pageable pageable)
    List<ProductSummaryDTO> getFeaturedProducts(int limit)

    // Stock
    @Transactional ProductResponseDTO updateStock(String id, StockUpdateDTO dto)
    List<ProductSummaryDTO> getLowStockProducts()
    List<ProductSummaryDTO> getOutOfStockProducts()

    // Dashboard / stats
    List<ProductDashboardDTO> getTopSellingProducts(int limit)
    List<ProductDashboardDTO> getTopRevenueProducts(int limit)
    ProductStatsDTO getProductStats()

    // Reference data
    List<String> getAllBrands()
    List<String> getAllTags()
    boolean skuExists(String sku)
    boolean skuExistsForOtherProduct(String sku, String excludeId)
}
```

### 6.7 Controller: `ProductController`

| Method | Path | Description |
|---|---|---|
| GET | `/products` | Paginated + filtered product list |
| GET | `/products/{id}` | Product by ID |
| POST | `/products` | Create product |
| PUT | `/products/{id}` | Full update |
| PATCH | `/products/{id}` | Partial update (map of field → value) |
| DELETE | `/products/{id}` | Delete product |
| GET | `/products/search?query=` | Search by name / sku / brand |
| GET | `/products/category/{category}` | Filter by category |
| GET | `/products/featured?limit=10` | Featured products |
| POST | `/products/{id}/stock` | Update stock |
| GET | `/products/stock/low` | Low stock products |
| GET | `/products/stock/out-of-stock` | Out of stock products |
| GET | `/products/dashboard/top-selling?limit=5` | Top sellers |
| GET | `/products/dashboard/top-revenue?limit=5` | Top revenue |
| GET | `/products/stats` | Aggregate statistics |
| GET | `/products/validate/sku?sku=&excludeId=` | SKU availability |
| GET | `/products/reference/brands` | All distinct brands |
| GET | `/products/reference/tags` | All distinct tags |
| POST | `/products/bulk-delete` | Delete multiple by ID array |
| POST | `/products/bulk-status` | Update status for multiple IDs |

**Filter query params for GET `/products`:** `search`, `status`, `category`, `stockStatus`, `brand`, `minPrice`, `maxPrice`, `minRating`, `featured`, `sortBy`, `sortDirection`, plus standard `page`, `size`, `sort` from `Pageable`.

---

## 7. Inventory Module

**Package:** `com.commerce_pro_backend.inventory`  
**Controller base path:** `/inventory`

### 7.1 Entities

#### `Warehouse`

**Table:** `warehouses`

| Field | Notes |
|---|---|
| `id` | `@UuidGenerator` |
| `name` | NOT NULL |
| `code` | nullable, unique by business logic |
| `address`, `city`, `state`, `country`, `postalCode` | address fields |
| `managerName`, `managerEmail`, `managerPhone` | contact info |
| `isActive` | default `true` |
| `isDefault` | default `false` — at most one per system |
| `createdAt`, `updatedAt` | `@PrePersist`, `@PreUpdate` |
| `inventories` | `@OneToMany` → `Inventory` |

#### `Inventory`

**Table:** `inventory`  
**Unique constraint:** `(product_id, warehouse_id)` — one record per product per warehouse

```java
@Entity @Table(name = "inventory")
public class Inventory {
    @Id @UuidGenerator String id;

    @ManyToOne Product product;      // FK → products
    @ManyToOne Warehouse warehouse;  // FK → warehouses

    Integer quantity;    // Total physical stock
    Integer reserved;    // Held for pending orders
    Integer available;   // Computed: quantity - reserved (never < 0)
    Integer incoming;    // Expected inbound

    Integer lowStockThreshold;
    Integer reorderPoint;
    Integer reorderQuantity;
    Integer maxStockLevel;
    Integer safetyStock;

    @Column(precision=19, scale=4) BigDecimal unitCost;
    @Column(precision=19, scale=4) BigDecimal totalValue;   // computed: unitCost × quantity

    String binLocation;
    String aisle;
    String zone;

    Boolean trackInventory;

    @Enumerated StockStatus status;    // IN_STOCK | LOW_STOCK | OUT_OF_STOCK | OVERSTOCK | NOT_TRACKED

    LocalDateTime lastRestocked;
    LocalDateTime lastCounted;

    @OneToMany(cascade=ALL, orphanRemoval=true)
    List<StockMovement> movements;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @PrePersist / @PreUpdate:
        updateAvailable()      // available = max(0, quantity - reserved)
        updateTotalValue()     // totalValue = unitCost × quantity
        updateStatus()         // sets StockStatus based on thresholds

    // Business methods
    void adjustStock(int newQty, String reason, String notes, String reference)
    void reserve(int amount)
    void releaseReservation(int amount)
}
```

**`StockStatus` enum:** `IN_STOCK`, `LOW_STOCK`, `OUT_OF_STOCK`, `OVERSTOCK`, `NOT_TRACKED`

#### `StockMovement`

**Table:** `stock_movements`

| Field | Notes |
|---|---|
| `id` | `@UuidGenerator` |
| `inventory` | FK → `inventory` |
| `product` | FK → `products` |
| `warehouse` | FK → `warehouses` |
| `type` | `MovementType` enum |
| `quantity` | the delta quantity |
| `previousQuantity` / `newQuantity` | before/after snapshot |
| `reason` | short description |
| `notes` | up to 2000 chars |
| `reference` | external reference number |
| `referenceType` | `ReferenceType` enum |
| `createdBy` | user ID |
| `createdAt` | `@PrePersist` |

**`MovementType` enum:** `IN`, `OUT`, `ADJUSTMENT`, `TRANSFER_IN`, `TRANSFER_OUT`, `RETURN`, `DAMAGED`  
**`ReferenceType` enum:** `PURCHASE_ORDER`, `SALES_ORDER`, `TRANSFER`, `ADJUSTMENT`, `RETURN`, `COUNT`

### 7.2 DTOs

| DTO | Purpose |
|---|---|
| `WarehouseRequestDTO` | Create/update warehouse |
| `WarehouseDTO` | Warehouse response |
| `InventoryRequestDTO` | Create/update inventory record |
| `InventoryDTO` | Full inventory response (includes nested product summary + warehouse) |
| `InventoryFilterDTO` | Filter: warehouseId, productId, status, category, trackInventory, lowStockOnly, outOfStockOnly, min/maxQuantity, min/maxValue, searchQuery |
| `InventoryStatsDTO` | Aggregate stats + per-warehouse breakdown |
| `StockUpdateRequestDTO` | Adjust stock: quantity, adjust (boolean), reason, notes, reference, referenceType |
| `StockTransferRequestDTO` | Transfer: fromWarehouseId, toWarehouseId, productId, quantity, notes, reference |
| `StockMovementDTO` | Movement log entry response |
| `LowStockAlertDTO` | Alert: product info, warehouse, stock levels, alert status, days until stockout |

### 7.3 Repository: `InventoryRepository`

```java
Optional<Inventory> findByProductIdAndWarehouseId(String productId, String warehouseId);
List<Inventory> findByProductId(String productId);
List<Inventory> findByWarehouseId(String warehouseId);
List<Inventory> findLowAndOutOfStock();
List<Inventory> findLowStock();
List<Inventory> findOutOfStock();
Integer getTotalQuantityByProductId(String productId);
BigDecimal getTotalInventoryValue();
long countByStatus(Inventory.StockStatus status);
List<Object[]> getStatsByWarehouse();   // grouping by warehouse
boolean existsByProductIdAndWarehouseId(String productId, String warehouseId);
```

### 7.4 `InventorySpecification`

Dynamic filter: `withFilter(InventoryFilterDTO)`, plus individual specs for `hasProductId`, `hasWarehouseId`, `hasStatus`, `isLowStock`, `isOutOfStock`.

### 7.5 Service: `InventoryService`

```java
@Service @RequiredArgsConstructor @Slf4j
@Transactional(readOnly = true)
public class InventoryService {
    // Warehouse CRUD
    List<WarehouseDTO> getAllWarehouses()
    List<WarehouseDTO> getActiveWarehouses()
    WarehouseDTO getWarehouseById(String id)
    @Transactional WarehouseDTO createWarehouse(WarehouseRequestDTO dto)   // handles default flag
    @Transactional WarehouseDTO updateWarehouse(String id, WarehouseRequestDTO dto)
    @Transactional void deleteWarehouse(String id)                         // rejects if has inventory

    // Inventory CRUD
    PageResponse<InventoryDTO> getInventory(InventoryFilterDTO filter, Pageable pageable)
    InventoryDTO getInventoryById(String id)
    InventoryDTO getInventoryByProductAndWarehouse(String productId, String warehouseId)
    List<InventoryDTO> getInventoryByProduct(String productId)
    List<InventoryDTO> getInventoryByWarehouse(String warehouseId)
    @Transactional InventoryDTO createInventory(InventoryRequestDTO dto)   // creates initial movement
    @Transactional InventoryDTO updateInventory(String id, InventoryRequestDTO dto)
    @Transactional void deleteInventory(String id)

    // Stock operations
    @Transactional InventoryDTO adjustStock(String inventoryId, StockUpdateRequestDTO dto)
    @Transactional void transferStock(StockTransferRequestDTO dto)       // creates TRANSFER_OUT + TRANSFER_IN movements

    // Stock movements
    List<StockMovementDTO> getStockMovements(String inventoryId)
    List<StockMovementDTO> getProductStockMovements(String productId)

    // Alerts
    List<InventoryDTO> getLowStockItems()
    List<InventoryDTO> getOutOfStockItems()

    // Statistics (aggregated from all Inventory records)
    InventoryStatsDTO getInventoryStats()

    // Private
    private void updateProductStock(Product product)   // syncs Product.stock = SUM(inventory quantities)
}
```

### 7.6 `LowStockService`

```java
@Service
public class LowStockService {
    List<LowStockAlertDTO> getLowStockAlerts()
    List<LowStockAlertDTO> getCriticalAlerts()        // OUT_OF_STOCK items
    List<LowStockAlertDTO> getAlertsByWarehouse(String warehouseId)
}
```

Alert status logic:
- `CRITICAL` — OUT_OF_STOCK
- `REORDER` — quantity ≤ reorderPoint (or lowStockThreshold if null)
- `LOW` — everything else in LOW_STOCK status

### 7.7 Controller: `InventoryController`

| Method | Path | Description |
|---|---|---|
| GET | `/inventory` | Paginated + filtered inventory |
| GET | `/inventory/{id}` | By ID |
| GET | `/inventory/product/{productId}` | All inventory for a product |
| GET | `/inventory/warehouse/{warehouseId}` | All inventory in a warehouse |
| POST | `/inventory` | Create inventory record |
| PUT | `/inventory/{id}` | Update inventory record |
| DELETE | `/inventory/{id}` | Delete inventory record |
| GET | `/inventory/warehouses` | All warehouses |
| GET | `/inventory/warehouses/active` | Active warehouses |
| GET | `/inventory/warehouses/{id}` | Warehouse by ID |
| POST | `/inventory/warehouses` | Create warehouse |
| PUT | `/inventory/warehouses/{id}` | Update warehouse |
| DELETE | `/inventory/warehouses/{id}` | Delete warehouse |
| POST | `/inventory/{id}/stock` | Adjust stock for inventory item |
| POST | `/inventory/transfer` | Transfer stock between warehouses |
| GET | `/inventory/{id}/movements` | Movement history for inventory |
| GET | `/inventory/product/{productId}/movements` | Movements for product |
| GET | `/inventory/stock/low` | Low stock items |
| GET | `/inventory/stock/out-of-stock` | Out of stock items |
| GET | `/inventory/alerts/low-stock` | Low stock alert DTOs |
| GET | `/inventory/alerts/critical` | Critical (out of stock) alerts |
| GET | `/inventory/alerts/warehouse/{warehouseId}` | Alerts by warehouse |
| GET | `/inventory/stats` | Inventory statistics |

---

## 8. Order Module

**Package:** `com.commerce_pro_backend.order`  
**Controller base path:** `/api/v1/orders`

### 8.1 Entities

#### `Order`

**Table:** `orders` — the aggregate root

**Key fields:**

| Field | Type | Notes |
|---|---|---|
| `id` | UUID (`@UuidGenerator`) | PK |
| `orderNumber` | VARCHAR(30) | UNIQUE — format `ORD-YYYYMM-NNNNN` (from `OrderNumberService`) |
| `customerId` | nullable | Guest orders allowed |
| `customerName`, `customerEmail`, `customerPhone` | snapshot at order time | |
| `status` | `OrderStatus` | Full state machine (18 states) |
| `paymentStatus` | `PaymentStatus` | 11 states |
| `fulfillmentStatus` | `FulfillmentStatus` | 4 states |
| `source` | `OrderSource` | STOREFRONT, MANUAL, API, IMPORT, MOBILE_APP |
| `subtotal`, `discountAmount`, `shippingCost`, `taxAmount`, `totalAmount`, `refundedAmount` | `BigDecimal(19,4)` | all NOT NULL |
| `couponCode`, `discountPercentage`, `currency` | | |
| `shippingAddress`, `billingAddress` | `@Embedded OrderAddress` | snapshotted at order time |
| `shippingMethod`, `trackingNumber`, `carrier` | | |
| `holdReason`, `cancellationReason`, `internalNotes`, `customerNotes` | | |
| `riskScore` | INTEGER, default 0 | auto-set by risk scorer |
| `isFlagged` | BOOLEAN, default false | auto-flagged for high-value/cross-border |
| `confirmedAt`, `shippedAt`, `deliveredAt`, `cancelledAt`, `closedAt` | milestone timestamps | |
| `createdAt`, `updatedAt`, `createdBy`, `updatedBy` | audit | |
| `items` | `@OneToMany` → `OrderItem` | cascade ALL, orphanRemoval |
| `statusHistory` | `@OneToMany` → `OrderStatusHistory` | cascade ALL, immutable |

**Indexes:** `order_number` (unique), `status`, `customer_id`, `created_at`, `payment_status`, `source`

**Business methods:**
```java
void recalculateTotals()              // recomputes subtotal + totalAmount from items
void transitionTo(OrderStatus, actorId, reason)  // records StatusHistory + sets milestone timestamps
boolean isEditable()                  // DRAFT | PENDING_PAYMENT | CONFIRMED
boolean isCancellable()               // not SHIPPED/DELIVERED/CANCELLED/CLOSED
int getTotalQuantity()                // sum of all item quantities
```

**`OrderStatus` state machine (18 states):**

`DRAFT` → `PENDING_PAYMENT` → `CONFIRMED` → `ON_HOLD` / `PROCESSING` → `PARTIALLY_FULFILLED` → `FULFILLED` → `SHIPPED` → `OUT_FOR_DELIVERY` → `DELIVERED` → `CLOSED`

Also: `PAYMENT_FAILED`, `CANCELLED`, `RETURN_INITIATED`, `RETURN_IN_TRANSIT`, `RETURN_RECEIVED`, `REFUNDED`, `PARTIALLY_REFUNDED`

#### `OrderItem`

**Table:** `order_items`

| Field | Notes |
|---|---|
| `id` | `@UuidGenerator` |
| `order` | `@ManyToOne` FK |
| `productId` | nullable (product may be deleted) |
| `sku`, `productName`, `productImageUrl`, `variantInfo` | product snapshot |
| `unitPrice`, `costPrice`, `itemDiscount`, `taxRate`, `taxAmount`, `lineTotal` | all `BigDecimal(19,4)` |
| `quantity`, `fulfilledQuantity`, `returnedQuantity` | |
| `createdAt` | |

**Business methods:**
```java
void recalculate()   // lineTotal = (unitPrice - itemDiscount) × qty + taxAmount
int getPendingFulfillmentQuantity()   // quantity - fulfilledQuantity
int getPendingReturnQuantity()        // fulfilledQuantity - returnedQuantity
```

#### `OrderAddress` (Embeddable)

Fields: `fullName`, `addressLine1`, `addressLine2`, `city`, `state`, `postalCode`, `country`, `phone`

Embedded twice in `Order` with `@AttributeOverrides` prefixes `ship_*` and `bill_*`.

#### `OrderStatusHistory`

**Table:** `order_status_history` — immutable audit trail

| Field | Notes |
|---|---|
| `id` | `@UuidGenerator` |
| `order` | `@ManyToOne` FK |
| `previousStatus` | `OrderStatus` enum |
| `newStatus` | `OrderStatus` enum |
| `reason` | VARCHAR(500) |
| `changedBy` | user ID |
| `createdAt` | `@CreationTimestamp` |

#### `OrderSequence`

**Table:** `order_sequences`

| Field | Notes |
|---|---|
| `sequenceKey` | PK VARCHAR(50) |
| `lastValue` | BIGINT, default 0 |
| `version` | `@Version` — optimistic locking |

Used by `OrderNumberService` with `PESSIMISTIC_WRITE` to generate unique order numbers.

### 8.2 Enums

```java
enum OrderStatus { DRAFT, PENDING_PAYMENT, PAYMENT_FAILED, CONFIRMED, ON_HOLD,
    PROCESSING, PARTIALLY_FULFILLED, FULFILLED, SHIPPED, OUT_FOR_DELIVERY,
    DELIVERED, CANCELLED, RETURN_INITIATED, RETURN_IN_TRANSIT, RETURN_RECEIVED,
    REFUNDED, PARTIALLY_REFUNDED, CLOSED }

enum PaymentStatus { PENDING, AUTHORIZED, CAPTURED, PARTIALLY_CAPTURED, FAILED,
    VOIDED, REFUND_PENDING, REFUNDED, PARTIALLY_REFUNDED, CHARGEBACK, DISPUTED }

enum FulfillmentStatus { UNFULFILLED, PARTIALLY_FULFILLED, FULFILLED, CANCELLED }

enum OrderSource { STOREFRONT, MANUAL, API, IMPORT, MOBILE_APP }
```

### 8.3 DTOs

| DTO | Purpose |
|---|---|
| `CreateOrderRequestDTO` | Create: customerName, email, phone, items, addresses, source, couponCode, shippingCost, discountAmount, shippingMethod, notes, currency |
| `UpdateOrderRequestDTO` | Update order (editable fields) |
| `OrderResponseDTO` | Full order: all fields, items, statusHistory, computed `isEditable`, `isCancellable` |
| `OrderSummaryDTO` | List: id, orderNumber, customer, status, totalAmount, itemCount, isFlagged, riskScore, timestamps |
| `OrderItemRequestDTO` | Line item for create: productId, quantity, taxRate, itemDiscount |
| `OrderItemResponseDTO` | Line item in response: all snapshot + computed fields |
| `OrderAddressDTO` | Address value object |
| `OrderStatusHistoryDTO` | Status history entry |
| `OrderFilterDTO` | Filter params: search, status, paymentStatus, source, isFlagged, date range, amount range |
| `OrderStatsDTO` | Counts by status + revenue totals |
| `OrderHoldRequest` | `reason` for placing on hold |
| `OrderCancelRequest` | `reason` for cancellation |
| `TrackingUpdateRequest` | `trackingNumber`, `carrier` for shipped transition |
| `BulkOrderActionRequest` | `ids[]` + action for bulk operations |

### 8.4 Repositories

```java
// OrderRepository
List<Order> findByStatus(OrderStatus status);
Page<Order> findAll(Specification<Order> spec, Pageable pageable);
// ... via JpaSpecificationExecutor

// OrderItemRepository
List<OrderItem> findByOrderId(String orderId);

// OrderSequenceRepository
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<OrderSequence> findBySequenceKey(String key);

// OrderStatusHistoryRepository
List<OrderStatusHistory> findByOrderIdOrderByCreatedAtDesc(String orderId);
```

### 8.5 Order State Machine — Controller Endpoints

| Method | Path | Transition | Permission |
|---|---|---|---|
| POST | `/api/v1/orders` | → DRAFT | `order:order:create` |
| GET | `/api/v1/orders` | — | `order:order:read` |
| GET | `/api/v1/orders/{id}` | — | `order:order:read` |
| PUT | `/api/v1/orders/{id}` | Editable orders | `order:order:update` |
| PATCH | `/api/v1/orders/{id}/tracking` | Update tracking | `order:order:update` |
| POST | `/api/v1/orders/{id}/confirm` | DRAFT → CONFIRMED | `order:order:manage-status` |
| POST | `/api/v1/orders/{id}/process` | CONFIRMED → PROCESSING | `order:order:manage-status` |
| POST | `/api/v1/orders/{id}/ship` | PROCESSING → SHIPPED | `order:order:manage-status` |
| POST | `/api/v1/orders/{id}/deliver` | SHIPPED → DELIVERED | `order:order:manage-status` |
| POST | `/api/v1/orders/{id}/hold` | CONFIRMED → ON_HOLD | `order:order:manage-status` |
| POST | `/api/v1/orders/{id}/release-hold` | ON_HOLD → CONFIRMED | `order:order:manage-status` |
| POST | `/api/v1/orders/{id}/close` | DELIVERED → CLOSED | `order:order:manage-status` |
| DELETE | `/api/v1/orders/{id}` | → CANCELLED | `order:order:cancel` |
| POST | `/api/v1/orders/bulk-action` | Multiple IDs | `order:order:bulk-action` |

---

## 9. Fulfillment Module

**Package:** `com.commerce_pro_backend.fulfillment`  
**Controllers:** `/api/v1/fulfillment` (FulfillmentController) + `/api/v1/shipments` (ShipmentController)

### 9.1 Entities

#### `Carrier`

**Table:** `carriers`

| Field | Notes |
|---|---|
| `id` | `@UuidGenerator` |
| `name` | VARCHAR(100), NOT NULL |
| `code` | VARCHAR(20), UNIQUE (e.g. `FEDEX`, `UPS`, `DHL`) |
| `status` | `CarrierStatus` (ACTIVE, INACTIVE), default ACTIVE |
| `trackingUrlTemplate` | e.g. `https://.../{trackingNumber}` |
| `logoUrl` | TEXT, nullable |
| `isDefault` | BOOLEAN, default false |
| `notes` | TEXT |
| `createdAt`, `updatedAt`, `createdBy`, `updatedBy` | audit |

#### `Shipment`

**Table:** `shipments`

| Field | Notes |
|---|---|
| `id` | `@UuidGenerator` |
| `shipmentNumber` | UNIQUE — format `SHP-YYYYMM-NNNNN` (from `ShipmentNumberService`) |
| `orderId`, `orderNumber` | denormalized FK snapshot |
| `carrierId`, `carrierName` | denormalized FK snapshot |
| `trackingNumber` | VARCHAR(100) |
| `status` | `ShipmentStatus` — 9 states |
| `shippingMethod`, `serviceLevel` | |
| `estimatedDeliveryDate`, `actualDeliveryDate` | |
| `recipientName`, `recipientPhone` | address snapshot |
| `addressLine1`, `addressLine2`, `city`, `state`, `postalCode`, `country` | |
| `weightGrams`, `lengthCm`, `widthCm`, `heightCm` | parcel dimensions |
| `shippingCost` | `BigDecimal(19,4)` |
| `labelUrl` | TEXT |
| `notes`, `exceptionReason` | |
| `createdAt`, `updatedAt`, `createdBy`, `updatedBy` | audit |
| `trackingEvents` | `@OneToMany` → `TrackingEvent` (cascade ALL) |

**Business methods:**
```java
void addTrackingEvent(TrackingEvent event)   // sets shipment status to event status; sets actualDeliveryDate if DELIVERED
boolean isDeletable()                        // only LABEL_CREATED
String getTrackingUrl(String template)       // fills {trackingNumber} in template
```

**`ShipmentStatus` enum:** `LABEL_CREATED`, `PICKED_UP`, `IN_TRANSIT`, `OUT_FOR_DELIVERY`, `DELIVERED`, `ATTEMPTED_DELIVERY`, `RETURNED_TO_SENDER`, `LOST`, `EXCEPTION`

#### `TrackingEvent`

**Table:** `tracking_events`

| Field | Notes |
|---|---|
| `id` | `@UuidGenerator` |
| `shipment` | `@ManyToOne` FK |
| `status` | `ShipmentStatus` enum |
| `location` | VARCHAR(300) |
| `description` | VARCHAR(1000), NOT NULL |
| `eventTime` | NOT NULL |
| `createdAt`, `createdBy` | |

#### `PickList`

**Table:** `pick_lists`

| Field | Notes |
|---|---|
| `id` | `@UuidGenerator` |
| `pickListNumber` | UNIQUE — format `PLK-YYYYMM-NNNNN` (from `PickListNumberService`) |
| `type` | `PickListType` (BATCH, SINGLE, CLUSTER, ZONE) |
| `status` | `PickListStatus` (GENERATED, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED) |
| `warehouseId`, `warehouseName` | denormalized |
| `assignedToUserId`, `assignedToName` | |
| `totalOrders`, `totalItems`, `completedItems` | counters |
| `notes` | TEXT |
| `generatedAt`, `assignedAt`, `startedAt`, `completedAt` | milestone timestamps |
| `createdAt`, `updatedAt`, `createdBy`, `updatedBy` | audit |
| `items` | `@OneToMany` → `PickListItem` (cascade ALL) |

**Business methods:**
```java
void refreshCounts()   // updates totalItems, completedItems from items list
boolean isAllPicked()  // all items have isPicked=true
boolean isCancellable() // GENERATED | ASSIGNED only
```

#### `PickListItem`

**Table:** `pick_list_items`

| Field | Notes |
|---|---|
| `id` | `@UuidGenerator` |
| `pickList` | `@ManyToOne` FK |
| `orderId`, `orderNumber`, `orderItemId` | references |
| `productId`, `sku`, `productName`, `variantInfo` | product snapshot |
| `binLocation` | warehouse location |
| `quantityRequired`, `quantityPicked` | |
| `isPicked` | BOOLEAN, default false |
| `notes` | |
| `createdAt` | |

```java
void markPicked(int qty)   // sets quantityPicked, isPicked = (picked >= required)
```

#### `ShippingRule`

**Table:** `shipping_rules`

| Field | Notes |
|---|---|
| `id` | `@UuidGenerator` |
| `name` | VARCHAR(200), NOT NULL |
| `description` | VARCHAR(1000) |
| `carrierId`, `carrierName` | optional FK to carrier |
| `conditionType` | `WEIGHT`, `PRICE`, `COUNTRY`, `ALWAYS` |
| `conditionMin`, `conditionMax` | `BigDecimal(19,4)` thresholds |
| `conditionValue` | free text (e.g. country codes) |
| `shippingMethod`, `serviceLevel` | outcome values |
| `priority` | lower = evaluated first, default 100 |
| `isActive` | default true |
| `createdAt`, `updatedAt`, `createdBy`, `updatedBy` | |

#### Sequence Entities

`ShipmentSequence` and `PickListSequence` follow the same pattern as `OrderSequence` — a single-row table with `PESSIMISTIC_WRITE` locking via `@Version` for race-safe reference number generation.

### 9.2 Services

#### `FulfillmentService`

```java
// Carriers
CarrierDTO createCarrier(CarrierRequestDTO dto)
List<CarrierDTO> getAllCarriers()
CarrierDTO getCarrier(String id)
CarrierDTO updateCarrier(String id, CarrierRequestDTO dto)
void deleteCarrier(String id)

// Shipping rules
ShippingRuleDTO createRule(ShippingRuleRequestDTO dto)
List<ShippingRuleDTO> getAllRules()
ShippingRuleDTO updateRule(String id, ShippingRuleRequestDTO dto)
void deleteRule(String id)

// Pick lists
PickListDTO generatePickList(CreatePickListRequest request)
PageResponse<PickListSummaryDTO> getPickLists(PickListFilterDTO filter, Pageable pageable)
PickListDTO getPickList(String id)
PickListDTO assignPickList(String id, AssignPickListRequest request)
PickListDTO startPickList(String id)
PickListDTO updatePickItem(String pickListId, String itemId, UpdatePickItemRequest request)
PickListDTO completePickList(String id)
void cancelPickList(String id)

// Fulfillment queue (CONFIRMED orders not yet in a pick list)
PageResponse<FulfillmentQueueItemDTO> getFulfillmentQueue(Pageable pageable)
FulfillmentStatsDTO getStats()
```

#### `ShipmentService`

```java
ShipmentDTO createShipment(CreateShipmentRequest request)
PageResponse<ShipmentSummaryDTO> getShipments(ShipmentFilterDTO filter, Pageable pageable)
ShipmentDTO getShipment(String id)
ShipmentDTO updateShipment(String id, UpdateShipmentRequest request)
ShipmentDTO addTrackingEvent(String shipmentId, AddTrackingEventRequest request)
ShipmentDTO markDelivered(String id)
void deleteShipment(String id)         // only LABEL_CREATED
ShipmentStatsDTO getShipmentStats()
```

### 9.3 Controllers

#### `FulfillmentController` — `/api/v1/fulfillment`

| Method | Path | Description | Permission |
|---|---|---|---|
| GET | `/queue` | Fulfillment queue (CONFIRMED orders) | `fulfillment:picklist:read` |
| GET | `/stats` | Fulfillment statistics | `fulfillment:stats` |
| GET | `/pick-lists` | Paginated pick lists | `fulfillment:picklist:read` |
| GET | `/pick-lists/{id}` | Pick list by ID | `fulfillment:picklist:read` |
| POST | `/pick-lists` | Generate pick list from orders | `fulfillment:picklist:create` |
| POST | `/pick-lists/{id}/assign` | Assign to user | `fulfillment:picklist:manage` |
| POST | `/pick-lists/{id}/start` | Mark as in progress | `fulfillment:picklist:manage` |
| PATCH | `/pick-lists/{id}/items/{itemId}` | Update item picked quantity | `fulfillment:picklist:manage` |
| POST | `/pick-lists/{id}/complete` | Complete pick list | `fulfillment:picklist:manage` |
| POST | `/pick-lists/{id}/cancel` | Cancel pick list | `fulfillment:picklist:manage` |
| GET | `/carriers` | All carriers | `fulfillment:carrier:read` |
| GET | `/carriers/{id}` | Carrier by ID | `fulfillment:carrier:read` |
| POST | `/carriers` | Create carrier | `fulfillment:carrier:manage` |
| PUT | `/carriers/{id}` | Update carrier | `fulfillment:carrier:manage` |
| DELETE | `/carriers/{id}` | Delete carrier | `fulfillment:carrier:manage` |
| GET | `/shipping-rules` | All shipping rules | `fulfillment:rules:read` |
| GET | `/shipping-rules/{id}` | Rule by ID | `fulfillment:rules:read` |
| POST | `/shipping-rules` | Create rule | `fulfillment:rules:manage` |
| PUT | `/shipping-rules/{id}` | Update rule | `fulfillment:rules:manage` |
| DELETE | `/shipping-rules/{id}` | Delete rule | `fulfillment:rules:manage` |

#### `ShipmentController` — `/api/v1/shipments`

| Method | Path | Description | Permission |
|---|---|---|---|
| GET | `/shipments` | Paginated shipments | `fulfillment:shipment:read` |
| GET | `/shipments/{id}` | Shipment by ID | `fulfillment:shipment:read` |
| POST | `/shipments` | Create shipment | `fulfillment:shipment:create` |
| PUT | `/shipments/{id}` | Update shipment | `fulfillment:shipment:update` |
| POST | `/shipments/{id}/tracking-events` | Add tracking event | `fulfillment:shipment:update` |
| POST | `/shipments/{id}/deliver` | Mark delivered | `fulfillment:shipment:manage` |
| DELETE | `/shipments/{id}` | Delete shipment (LABEL_CREATED only) | `fulfillment:shipment:delete` |

---

## 10. User Identity Module

**Package:** `com.commerce_pro_backend.user_identity`  
**Controller base paths:** `/api/v1/auth/...`, `/api/v1/identity/...`

### 10.1 Entities

#### `User`

**Table:** `users`

```java
@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = UUID) String id;
    @Column(unique=true, nullable=false, length=50) String username;
    @Column(unique=true, nullable=false, length=100) String email;
    @Column(length=255) String passwordHash;
    String firstName, lastName, phone;
    Boolean isActive;            // default true
    Boolean isEmailVerified;     // default false
    Boolean mfaEnabled;          // default false
    String mfaSecret;            // encrypted via MfaSecretEncryptor pattern
    Integer failedLoginAttempts; // default 0
    LocalDateTime lockedUntil;   // null = not locked
    LocalDateTime passwordChangedAt;
    Boolean mustChangePassword;  // default false
    LocalDateTime lastLoginAt;
    String lastLoginIp;

    @OneToMany(cascade=ALL, orphanRemoval=true)
    Set<UserRoleAssignment> roleAssignments;

    @CreationTimestamp LocalDateTime createdAt;
    @UpdateTimestamp  LocalDateTime updatedAt;
    String createdBy, updatedBy;

    // Business methods
    boolean hasSuperAdminRole()
    Set<Role> getActiveRoles()     // filters by status=ACTIVE + validity dates
    void assignRole(Role, assignedBy, validFrom, validUntil)
}
```

#### `Role`

**Table:** `roles`

```java
@Entity @Table(name = "roles")
public class Role {
    @Id @GeneratedValue(UUID) String id;
    @Column(unique=true, nullable=false, length=50) String code;   // e.g. "SUPER_ADMIN"
    @Column(nullable=false, length=100) String name;
    @Column(length=500) String description;
    @Enumerated RoleType type;     // SYSTEM | GLOBAL | TENANT | RESOURCE | DYNAMIC
    Boolean isSystem;              // cannot be deleted
    Boolean isSuperAdmin;          // special bypass flag

    @ManyToOne Role parentRole;    // role hierarchy
    @OneToMany Set<Role> childRoles;

    @ManyToMany(fetch=EAGER)
    @JoinTable("role_permissions")
    Set<Permission> permissions;

    @ElementCollection Set<String> constraints;  // conditional access JSON rules
    Integer maxAssignmentDurationDays;           // null = unlimited
    Boolean requiresMfa;
    Integer sessionTimeoutMinutes;
    String allowedIpPatterns;    // CIDR notation comma-separated
    String timeRestrictions;     // cron / JSON

    // Business methods
    boolean isAssignableBy(Role assignerRole)
    Set<Permission> getAllPermissions()  // includes inherited from parentRole
}
```

#### `Permission`

**Table:** `permissions`

```java
@Entity @Table(name = "permissions")
public class Permission {
    @Id @Column(name="code", length=100) String code;   // e.g. "identity:user:create"
    @Column(nullable=false, length=100) String name;
    @Column(length=500) String description;
    @Enumerated PermissionCategory category;
    Boolean isSystem;             // cannot be deleted
    Boolean requiresApproval;
    Integer riskLevel;            // 1–5
    @ElementCollection Set<String> applicableScopes;  // any, own, department, etc.
    @ManyToMany(mappedBy="permissions") Set<Role> roles;
}
```

**Permission code format:** `<module>:<resource>:<action>` e.g. `identity:user:create`, `order:order:read`, `fulfillment:shipment:manage`

#### `UserRoleAssignment`

**Table:** `user_role_assignments`

| Field | Notes |
|---|---|
| `id` | UUID generated |
| `user` | FK → users |
| `role` | FK → roles |
| `status` | `AssignmentStatus` enum |
| `validFrom`, `validUntil` | time-bounded roles |
| `assignedBy` | user ID of admin |
| `assignedAt` | `@CreationTimestamp` |
| `revokedBy`, `revokedAt`, `revocationReason` | revocation audit |
| `constraints` | `@ElementCollection` — additional rules |
| `scopeContext` | JSON — scoped access context |

```java
boolean isCurrentlyValid()  // checks status=ACTIVE + validity dates
```

#### `AuditLog`

**Table:** `audit_logs`

```java
@Entity @Table(name = "audit_logs")
public class AuditLog {
    @Id @GeneratedValue(UUID) String id;
    @Enumerated AuditAction action;
    String actorId;           // who performed the action
    String actorRole;         // role at time of action
    String targetType;        // USER, ROLE, PERMISSION, ORDER, etc.
    String targetId;
    String targetIdentifier;  // human-readable (username, order number)
    @Column(length=1000) String actionDescription;
    @Column(length=4000) String oldValue;   // JSON previous state
    @Column(length=4000) String newValue;   // JSON new state
    String ipAddress;
    String userAgent;
    String sessionId;
    Boolean success;
    String failureReason;
    String requestId;
    @CreationTimestamp LocalDateTime timestamp;
    Long processingTimeMs;
}
```

### 10.2 Enums

```java
// Permission categories
enum PermissionCategory {
    IDENTITY_MANAGEMENT, SYSTEM_CONFIGURATION, AUDIT_AND_COMPLIANCE,
    SECURITY_OPERATIONS, API_MANAGEMENT, INTEGRATION_CONFIG,
    ORDER_MANAGEMENT, FULFILLMENT_MANAGEMENT
}

// All audit actions
enum AuditAction {
    USER_CREATED, USER_UPDATED, USER_DELETED, USER_ACTIVATED, USER_DEACTIVATED,
    USER_LOCKED, USER_UNLOCKED, PASSWORD_CHANGED, PASSWORD_RESET, USER_IMPERSONATED,
    ROLE_CREATED, ROLE_UPDATED, ROLE_DELETED, ROLE_ASSIGNED, ROLE_REVOKED,
    PERMISSION_GRANTED, PERMISSION_REVOKED, PERMISSION_CREATED, PERMISSION_UPDATED, PERMISSION_DELETED,
    LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT, SESSION_TERMINATED,
    MFA_ENABLED, MFA_DISABLED, MFA_VERIFIED,
    CONFIG_UPDATED, POLICY_CHANGED,
    AUDIT_VIEWED, REPORT_GENERATED, AUDIT_PURGED, AUDIT_EXPORTED,
    ORDER_CREATED, ORDER_UPDATED, ORDER_CONFIRMED, ORDER_CANCELLED, ORDER_SHIPPED,
    ORDER_DELIVERED, ORDER_CLOSED, ORDER_HELD, ORDER_HOLD_RELEASED,
    ORDER_STATUS_CHANGED, ORDER_TRACKING_UPDATED, ORDER_BULK_ACTION,
    FULFILLMENT_PICKLIST_CREATED, FULFILLMENT_PICKLIST_ASSIGNED, FULFILLMENT_PICKLIST_STARTED,
    FULFILLMENT_PICKLIST_COMPLETED, FULFILLMENT_PICKLIST_CANCELLED,
    FULFILLMENT_SHIPMENT_CREATED, FULFILLMENT_SHIPMENT_UPDATED, FULFILLMENT_SHIPMENT_TRACKING_ADDED,
    FULFILLMENT_SHIPMENT_DELETED, FULFILLMENT_CARRIER_CREATED, FULFILLMENT_CARRIER_UPDATED,
    FULFILLMENT_CARRIER_DELETED, FULFILLMENT_RULE_CREATED, FULFILLMENT_RULE_UPDATED, FULFILLMENT_RULE_DELETED
}

// Role classification
enum RoleType { SYSTEM, GLOBAL, TENANT, RESOURCE, DYNAMIC }

// Assignment lifecycle
enum AssignmentStatus { ACTIVE, SUSPENDED, EXPIRED, REVOKED, PENDING_APPROVAL }
```

### 10.3 Key Services

#### `JwtTokenProvider`

- Signs JWT with HS512 using `app.jwt.secret`
- Access token: 15 minutes (`app.jwt.expiration-ms=900000`)
- Refresh token: 7 days (`app.jwt.refresh-expiration-ms=604800000`)
- Impersonation token: 30 minutes

#### `JwtAuthenticationFilter`

Runs on every request. Extracts `Authorization: Bearer <token>`, validates via `JwtTokenProvider`, loads user details from `CustomUserDetailsService`, sets `Authentication` in `SecurityContextHolder`.

#### `SuperAdminAuthorizationFilter`

Runs **before** `JwtAuthenticationFilter`. Performs super-admin-specific checks (IP restriction in dev, rate limiting, etc.).

#### `CurrentUserService`

**Always use this in services to get the current user ID — never `SecurityContextHolder` directly.**

```java
@Service
public class CurrentUserService {
    public String getCurrentUserId() { /* reads from SecurityContextHolder */ }
    public User getCurrentUser()     { /* loads full User entity */ }
}
```

#### `AuditService`

```java
@Service
public class AuditService {
    void log(String actorId, AuditAction action, String targetType, String targetId,
             String targetIdentifier, String oldValue, String newValue, boolean success);
}
```

Call this in every service method that performs a state-changing operation.

#### `TotpService`

Handles TOTP-based MFA using `commons-codec` Base32 for secret generation and time-window validation.

#### `AuthService`

Handles login (returns access + refresh tokens), logout, token refresh, MFA setup/verification/disable, password change, and user impersonation.

### 10.4 Controllers

#### `AuthController` — `/api/v1/auth`

| Method | Path | Auth Required | Description |
|---|---|---|---|
| POST | `/auth/login` | No | Returns `AuthResponse` with access + refresh tokens |
| POST | `/auth/refresh` | No | Exchange refresh token for new access token |
| POST | `/auth/logout` | Yes | Invalidate current session |
| POST | `/auth/mfa/setup` | Yes | Generate TOTP secret + QR code |
| POST | `/auth/mfa/verify` | Yes | Verify TOTP code |
| POST | `/auth/mfa/disable` | Yes | Disable MFA |

**`AuthResponse` shape:** `{ accessToken, refreshToken, tokenType, expiresIn, user: { id, username, email, roles, permissions } }`

#### `UserController` — `/api/v1/identity/users`

CRUD for users + role assignment management.

#### `RoleController` — `/api/v1/identity/roles`

CRUD for roles + permission management + hierarchy management.

#### `PermissionController` — `/api/v1/identity/permissions`

CRUD for permissions (system permissions cannot be deleted).

#### `AuditController` — `/api/v1/identity/audit`

Read audit logs with filtering, export, purge.

#### `ConfigurationController` — `/api/v1/identity/config`

Super-admin-only: read/update system configuration and security policies.

---

## 11. Security Architecture

### 11.1 Filter Chain (in order of execution)

```
Request
  ↓ SuperAdminAuthorizationFilter    (IP checks, rate limiting for super admin)
  ↓ JwtAuthenticationFilter          (extract + validate JWT → set SecurityContext)
  ↓ UsernamePasswordAuthenticationFilter (standard Spring Security)
  ↓ SecurityFilterChain (URL authorization rules)
```

### 11.2 URL Authorization Rules (from `SecurityConfig`)

```java
// Public
/h2-console/**              → permitAll
/api/v1/auth/login          → permitAll
/api/v1/auth/refresh        → permitAll
/actuator/health            → permitAll
/swagger-ui/**              → permitAll
/v3/api-docs/**             → permitAll

// Super admin only
/api/v1/admin/**            → hasRole("SUPER_ADMIN")
/api/v1/identity/config/**  → hasRole("SUPER_ADMIN")
/api/v1/identity/audit/**   → hasAnyRole("SUPER_ADMIN", "AUDIT_ADMIN")

// Identity — granular permission checks
POST /api/v1/identity/users                  → hasAuthority("identity:user:create")
DELETE /api/v1/identity/users/**             → hasAuthority("identity:user:delete")
GET /api/v1/identity/roles/**                → hasAuthority("identity:role:read")
// ... etc.

// Orders
GET /api/v1/orders/**                        → hasAuthority("order:order:read")
POST /api/v1/orders                          → hasAuthority("order:order:create")
POST /api/v1/orders/*/confirm                → hasAuthority("order:order:manage-status")
// ... etc.

// Fulfillment — see SecurityConfig for full mapping

// Everything else
anyRequest() → authenticated()
```

### 11.3 `PermissionRegistry`

On startup (`@PostConstruct`), registers all system permissions in memory:

| Category | Example Permissions |
|---|---|
| `IDENTITY_MANAGEMENT` | `identity:user:create/read/update/delete`, `identity:role:*`, `identity:permission:*` |
| `SYSTEM_CONFIGURATION` | `identity:config:read/update`, `identity:config:security-policy` |
| `AUDIT_AND_COMPLIANCE` | `identity:audit:read/export/purge/configure` |
| `SECURITY_OPERATIONS` | `identity:security:session-manage/force-logout/block-ip/unlock-account` |
| `API_MANAGEMENT` | `identity:api-key:create/revoke`, `identity:webhook:configure` |
| `INTEGRATION_CONFIG` | `identity:integration:sso-config/ldap-config` |
| `ORDER_MANAGEMENT` | `order:order:read/create/update/cancel/manage-status/bulk-action/export/flag/view-financials/stats` |
| `FULFILLMENT_MANAGEMENT` | `fulfillment:picklist:read/create/manage`, `fulfillment:shipment:read/create/update/manage/delete`, `fulfillment:carrier:read/manage`, `fulfillment:rules:read/manage`, `fulfillment:stats` |

**Total: 67 registered permissions**

### 11.4 Method Security

Enabled via `@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)`.

Use `@PreAuthorize("hasAuthority('permission:code')")` on service or controller methods for fine-grained security.

---

## 12. Data Initializer

**Class:** `DataInitializer`  
**Profile:** `@Profile("dev")` only  
**Execution:** `@Order` beans as `CommandLineRunner`

DDL is `create-drop`, so the schema is rebuilt fresh on every startup. The initializer then seeds data in this order:

### Order 1: Products (`seedProducts`)

Creates 22 products across categories: Electronics (9), Fashion (5), Home & Kitchen (4), Sports (4). Three products have variants (Color for headphones, Size+Color for sneakers and running shoes).

### Order 2: Inventory (`seedInventory`)

Creates 2 warehouses:
- **Main Warehouse** (`MAIN-001`) — New York, NY — isDefault=true
- **West Coast Warehouse** (`WEST-002`) — Los Angeles, CA

Seeds inventory for all products. Main warehouse gets 70–100% of stock; every 3rd product also gets 20–30% in the West Coast warehouse.

### Order 3: Orders (`seedOrders`)

Uses a security context fix: installs a synthetic `Authentication` for `superadmin` so `CurrentUserService.getCurrentUserId()` resolves during `CommandLineRunner` startup (no HTTP request / JWT exists).

Creates 10 scenario orders:

| Scenario | Customer | Status | Notes |
|---|---|---|---|
| 1 | Alice Johnson | DRAFT | Standard storefront order |
| 2 | Bob Smith | PENDING_PAYMENT | Express shipping |
| 3 | Carol Williams | CONFIRMED | With coupon `SAVE10` |
| 4 | David Brown | ON_HOLD | Billing verification |
| 5 | Eve Davis | PROCESSING | |
| 6 | Frank Miller | SHIPPED | UPS tracking `UPS1234567890` |
| 7 | Grace Lee | DELIVERED | FedEx tracking |
| 8 | Henry Wilson | CLOSED | DHL, with coupon `WELCOME15` |
| 9 | Irene Taylor | CANCELLED | Customer requested |
| 10 | James Anderson | DRAFT (auto-flagged) | Cross-border, high-value — triggers risk scorer |

### Order 4: Fulfillment (`seedFulfillment`)

Creates 4 carriers: Manual/In-House (default), FedEx, UPS, DHL Express.

Creates 2 shipping rules: Standard Domestic (ALWAYS), Express Shipping (PRICE ≥ $100).

Creates 1 pick list from 2 CONFIRMED orders.

Creates 1 shipment for the first SHIPPED order with a tracking event.

---

## 13. Cross-Module Integration Map

```
user_identity ←── All modules: AuditService.log() for state changes
user_identity ←── All modules: SecurityConfig / PermissionRegistry for authorization

catalog/product ←── inventory  (Inventory.product FK)
catalog/product ←── inventory  (StockMovement.product FK)
catalog/product ←── order      (OrderItem.productId + product snapshot)

inventory ←── order   (stock reservation on confirmOrder, release on cancel)
order     ←── fulfillment (PickListItem.orderId + orderNumber)
order     ←── fulfillment (Shipment.orderId + orderNumber)

fulfillment    ←── user_identity (AuditService for all WMS actions)
order          ←── user_identity (AuditService for all order transitions)

common/storage ←── catalog (product images upload)
```

**Data sync pattern:**  
`Inventory.quantity` is the source of truth for stock in the warehouse. `InventoryService.updateProductStock()` syncs this back to `Product.stock` (the aggregate sum across all warehouses) whenever inventory is created, updated, adjusted, or deleted.

---

## 14. Architectural Rules Reference

These rules are **non-negotiable** in the codebase:

| Rule | Description |
|---|---|
| **UUIDs only** | All entity IDs use `@UuidGenerator` or `@GeneratedValue(UUID)` — never Long auto-increment |
| **BigDecimal money** | All monetary fields: `@Column(precision=19, scale=4)` — never `double` or `float` |
| **ApiException only** | All service errors use `ApiException.notFound()`, `.badRequest()`, `.conflict()`, `.unauthorized()`, `.forbidden()` — never raw `RuntimeException` |
| **No entity exposure** | Controllers always return DTOs — never expose JPA entities as API responses |
| **CurrentUserService** | In services, always use `CurrentUserService.getCurrentUserId()` — never `SecurityContextHolder.getContext()` directly |
| **File serving** | All file downloads go through `FileStorageController GET /api/files/download/{fileName:.+}` — no new file-serving endpoints |
| **Encryption pattern** | Field-level encryption (e.g. MFA secrets, gateway credentials) must reuse the `MfaSecretEncryptor @Convert` pattern |
| **Sequence + PESSIMISTIC_WRITE** | All business reference numbers (order numbers, shipment numbers, pick list numbers) use the sequence-table + `PESSIMISTIC_WRITE` pattern |
| **AuditService** | Every significant state-changing operation must call `AuditService.log()` |
| **New module checklist** | Adding a module requires updates to: `PermissionCategory`, `AuditAction`, `PermissionRegistry.init()`, and `SecurityConfig.filterChain()` |
| **Design doc first** | Any new module or major feature requires a full design document before implementation begins |
| **Manual mapper default** | Manual mapper classes (`@Component`, plain Java) are the default; MapStruct is available but must be explicitly chosen for new modules |
| **@Transactional** | Services have `@Transactional(readOnly = true)` at class level; mutating methods override with `@Transactional` |
| **Controller delegates** | Controllers contain zero business logic — all logic is in the service layer |
| **Builder.Default** | Collections on entities use `@Builder.Default` to initialize to empty lists/sets |
| **@PrePersist / @PreUpdate** | Derived fields (stockStatus, available quantity, totalValue) are computed in lifecycle callbacks |
