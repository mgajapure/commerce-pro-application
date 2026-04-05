# AGENTS.md — Commerce Pro AI Coding Guide

> **Last Updated:** April 4, 2026  
> **Stack:** Angular 21 (Frontend) · Spring Boot 3.5.11 + Java 21 (Backend) · Groq LLM (AI)  
> **Database:** H2 (dev) / PostgreSQL (prod via Render)

---

## 🏗️ Architecture Overview

**Commerce Pro** is a three-tier e-commerce application with integrated AI capabilities:

1. **Frontend** (`commerce-pro-admin-frontend/`): Angular 21 standalone app with Tailwind CSS
   - Compiled to static assets, served via Render
   - Standalone components (no NgModules)
   - Routes defined in `app.routes.ts`
   - State via RxJS signals, no NgRx

2. **Backend** (`commerce-pro-backend/`): Spring Boot 3.5.11 microservice
   - REST API at `/api/**` context path
   - JPA/Hibernate ORM (8 business domains)
   - Groq LLM integration (OpenAI-compatible) for AI features
   - Deployed as Docker container on Render

3. **Database**: PostgreSQL (Render) with H2 fallback for local dev
   - `createDrop` DDL auto-generation — schema resets on startup
   - Connection pooling: HikariCP (max 20 connections)

**Deployment Topology** (render.yaml):
```
PostgreSQL → Spring Boot (Docker) → Angular (Static)
                ↓
          /api/actuator/health (health check)
```

---

## 🧠 AI Module Architecture

The AI system is **NOT part of Anthropic/Claude** (as originally documented). It uses **Groq** (OpenAI-compatible API) with **Spring AI** framework:

### Key Files & Components

| Component | File | Purpose |
|-----------|------|---------|
| **Configuration** | `src/main/resources/application.properties` (lines 165–214) | Groq API key, model aliases, budgets, timeouts |
| **Spring AI Config** | `ai/` directory (to be created) | Bean definitions, client setup |
| **Features** | `ai/features/` | 15 domain-specific AI services (fraud, forecast, churn, etc.) |
| **Entities** | `ai/entity/` | `AiInsight`, `AiConversation`, `AiUsageLog` (schema auto-created) |
| **Orchestrator** | `ai/service/AiOrchestrator.java` | Builds requests, calls Groq SDK, routes responses |
| **Memory Manager** | `ai/service/AiMemoryManager.java` | Context window budgeting |
| **Cost Guard** | `ai/service/AiCostGuard.java` | Daily spend limits (`ai.budget.daily-total-usd=50.00`) |

### Models Used

- **Fast**: `llama-3.1-8b-instant` (fraud, sentiment, description, SEO, shipping)
- **Balanced**: `llama-3.3-70b-versatile` (default)
- **Powerful**: `llama-3.3-70b-versatile` (reserved for complex reasoning)

### Request Pattern

All AI features follow this flow:
```
BusinessService.method()
  → AiOrchestrator.analyze(featureType, context)
    → AiCostGuard.checkBudget() + AiRateLimiter.checkQuota()
    → Build prompt (system + user)
    → Call Groq SDK
    → Parse JSON response
    → Persist to AiInsight table
  → Return result to frontend
```

---

## 🗂️ Project Structure Highlights

### Backend Package Layout
```
src/main/java/com/commerce_pro_backend/
├── ai/                          ← AI module (15 features)
│   ├── config/
│   ├── entity/                  ← AiInsight, AiConversation, AiUsageLog
│   ├── repository/
│   ├── service/                 ← Orchestrator, MemoryManager, CostGuard
│   └── features/                ← Fraud, Forecast, Churn, Reports, etc.
├── catalog/                     ← Products, Categories, Reviews
├── customer/                    ← Customers, Loyalty
├── order/                       ← Orders, Items
├── payment/                     ← PaymentTransaction, PaymentMethod
├── inventory/                   ← Warehouse, Stock
├── analytics/                   ← Metrics, Reports
├── common/                      ← Shared utils, exceptions, responses
├── system/                      ← Audit, Health, Config
└── user_identity/              ← Users, Roles, JWT
```

### Frontend Component Structure
```
src/app/
├── features/                    ← Feature modules (lazy-loaded)
├── core/                        ← Singletons: AuthService, HttpInterceptor
├── shared/                      ← Reusable components, pipes, directives
└── app.config.ts               ← Route definitions (standalone API)
```

---

## 🔧 Critical Build & Runtime Workflows

### Backend Build (Gradle)
```bash
# Development (with devtools for hot reload)
./gradlew bootRun -Dspring.profiles.active=dev

# Production build
./gradlew clean build -x test

# Test
./gradlew test

# Dockerfile available at: commerce-pro-backend/Dockerfile
# Uses: Eclipse Temurin OpenJDK 21, multi-stage build
```

### Frontend Build (Angular)
```bash
# Development server (watches & rebuilds)
npm start                    # ng serve

# Production build
npm run build               # ng build --configuration production
# Output: dist/commerce-pro/browser/ (static files for Render)

# Tests
npm test                    # ng test (Vitest)
```

### Database Setup
- **H2 (local)**: Auto-created at `./data/commerce-pro-db.mv.db`
  - Console at `http://localhost:8080/api/h2-console`
  - Credentials: `admin` / `admin`
- **PostgreSQL (prod)**: Auto-created on Render via `DATABASE_URL` env var

### Deployment (Render.yaml)
```yaml
Order: DB → Backend (Docker) → Frontend (Static)
Backend health check: GET /api/actuator/health
Frontend routing: All non-file requests → index.html (SPA fallback)
```

---

## 📋 Project-Specific Conventions & Patterns

### 1. **Lombok Usage**
- All entities, DTOs, and services use `@Data`, `@RequiredArgsConstructor`, `@Builder`
- Annotation processor configured in `build.gradle` (line 60)
- MapStruct + Lombok binding enabled (line 68)

### 2. **DTO Mapping (MapStruct)**
- Every entity has a corresponding `*DTO` and `*Mapper` interface
- Mapper auto-generates implementations at compile time
- Pattern: `PaymentTransaction` ↔ `PaymentTransactionDTO`
- Config: MapStruct set to Spring component model, ignore unmapped targets

### 3. **JPA/Hibernate Patterns**
- DDL: `create-drop` (schema regenerated on every startup)
- **Lazy loading**: Disable for production; use `@Transactional` or explicit fetch
- Bidirectional relationships: Use `mappedBy` on "many" side; manage both sides in code
- Soft delete: Not implemented; use status columns instead

### 4. **API Response Format**
```java
// All REST endpoints return a standard wrapper
{
  "success": true,
  "data": { /* actual payload */ },
  "message": "Operation completed",
  "errors": []
}
```

### 5. **Error Handling**
- Custom exceptions: `BusinessException`, `ResourceNotFoundException`, `ValidationException`
- Global `@RestControllerAdvice` in `common/` catches and formats errors
- HTTP status mapping: 400 (validation), 404 (not found), 500 (server error)

### 6. **Authentication (JWT)**
- Token in `Authorization: Bearer <token>` header
- `JwtUtil` generates/validates tokens (configured in `application-identity.properties`)
- `AuthFilter` intercepts all `/api/**` requests except login/register
- Two roles: `ADMIN`, `USER` (configured in `User.role` enum)

### 7. **Angular Standalone Components**
- No NgModules; components marked with `standalone: true`
- Imports declared inline in component decorator
- Routes in `app.routes.ts` use `children` array for feature modules
- No module-level lazy loading; use `loadComponent` / `loadChildren` in routes

### 8. **Reactive Forms (Angular)**
- `FormBuilder` or `new FormGroup([...])` patterns
- Validation via `Validators` (built-in) + custom async validators
- Status/value changes via `.valueChanges`, `.statusChanges` observables
- No Template-Driven Forms

### 9. **TypeScript Strict Mode**
- `tsconfig.json`: `strict: true`, `noImplicitAny: true`
- All Angular services use `inject()` function (Angular 14+) instead of constructor injection

### 10. **Styling (Tailwind CSS)**
- v4.2.1 with PostCSS
- No SCSS variables; use Tailwind utility classes
- Custom colors in `tailwind.config.js` if needed
- Print styles: `@media print` in inline `<style>`

---

## 🚨 Critical Integration Points

### Backend ↔ Frontend
- **Base URL**: `NG_APP_API_URL` (set at build time from env var)
- **CORS**: Configured in `CorsConfig` (allowed origins in `application.properties`)
- **Authentication**: JWT token in `Authorization` header, intercepted by `AuthService`

### Backend ↔ Groq AI
- **Endpoint**: `https://api.groq.com/openai/v1`
- **API Key**: `GROQ_API_KEY` env var (application.properties line 167)
- **Spring AI**: Handles OpenAI-compatible protocol wrapping
- **Timeout**: 30s read, 5s connect (lines 194–195)

### Backend ↔ Database
- **Local**: H2 file-based (auto-created, DDL reset on startup)
- **Production**: PostgreSQL via `DATABASE_URL` connection string
- **Connection Pool**: HikariCP, 20 max connections, 300s idle timeout
- **Transactions**: `@Transactional` on service methods; rollback on unchecked exceptions

### Payment Processing
- **Credentials**: Encrypted via AES-256-GCM (key in `PAYMENT_ENCRYPTION_KEY` env var)
- **Linked**: `PaymentTransaction` → `PaymentMethod` (one-to-many)
- **AI Scoring**: Synchronously triggered on transaction creation

---

## 🔐 Environment Variables (Render Deployment)

**Set in Render Dashboard** (marked `sync: false` in render.yaml):
```bash
# Security
JWT_SECRET=<64+ char hex string>
MFA_ENCRYPTION_KEY=<32+ chars>
ADMIN_PASSWORD=<new admin password>
SUPERADMIN_PASSWORD=<new superadmin password>
PAYMENT_ENCRYPTION_KEY=<AES-256 key>

# AI
GROQ_API_KEY=<from https://console.groq.com>

# Email
MAIL_USERNAME=<gmail address>
MAIL_PASSWORD=<Gmail App Password, not account password>
MAIL_FROM=<sender@domain>

# CORS (auto-set by Render)
CORS_ALLOWED_ORIGINS=https://commerce-pro-admin-frontend.onrender.com
FRONTEND_URL=https://commerce-pro-admin-frontend.onrender.com
```

---

## 📚 Key Documentation References

| Document | Location | What to Read |
|----------|----------|--------------|
| **AI Features** | `docs/ai/01-features-catalog.md` | All 15 AI features, inputs/outputs, prompts |
| **Memory Strategy** | `docs/ai/02-memory-management.md` | Context windowing, session TTLs, cached prompts |
| **Database Schema** | `docs/ai/03-entities-schema.md` | `AiInsight`, `AiConversation`, `AiUsageLog` fields |
| **Infrastructure** | `docs/ai/04-infrastructure.md` | `AiOrchestrator`, `AiMemoryManager`, `AiCostGuard` code patterns |
| **Implementation** | `docs/ai/05-implementation-guide.md` | Phased rollout, dependency order, testing |
| **App Understanding** | `documents/commerce-pro-application-understanding-document.pdf` | Full system overview, workflows, entity relationships |

---

## 🎯 Common Tasks for Agents

### Adding a New Feature Endpoint

1. **Backend**:
   ```java
   // 1. Create DTO in common/dto/
   @Data public class FeatureRequestDTO { ... }
   
   // 2. Create Service in appropriate domain package/
   @Service public class FeatureService { ... }
   
   // 3. Create REST Controller in appropriate domain package/
   @RestController @RequestMapping("/api/v1/feature")
   public class FeatureController {
       @PostMapping public ResponseEntity<?> create(@RequestBody FeatureRequestDTO req) { ... }
   }
   
   // 4. Wire: Controller → Service → Repository → Database
   ```

2. **Frontend**:
   ```typescript
   // 1. Create service in features/feature/services/
   @Injectable() export class FeatureService {
       constructor(private http = inject(HttpClient)) {}
       create(payload) { return this.http.post('/api/v1/feature', payload); }
   }
   
   // 2. Create component in features/feature/components/
   // 3. Add route in app.routes.ts
   ```

### Integrating AI Feature

1. Read `docs/ai/01-features-catalog.md` for the feature spec
2. Create `FeatureService` in `ai/features/{domain}/`
3. Call `aiOrchestrator.analyze(featureType, context)` from business service
4. Response auto-persists to `ai_insights` table
5. Frontend fetches insights via `/api/v1/ai/{feature}/insights`

### Debugging

- **Backend**: Logs at `INFO` level (lines 47–50 in application.properties)
- **Database**: H2 console at `http://localhost:8080/api/h2-console`
- **Frontend**: Angular DevTools browser extension
- **Groq Calls**: Add logging in `AiOrchestrator` before/after SDK call

---

## ⚡ Quick Command Reference

```bash
# Backend
cd commerce-pro-backend
./gradlew bootRun                    # Start with hot reload
./gradlew test                       # Run tests
./gradlew build -x test              # Build JAR (skips tests)

# Frontend
cd commerce-pro-admin-frontend
npm install                          # Install deps
npm start                            # Dev server at localhost:4200
npm run build                        # Production build
npm test                             # Run tests

# Docker (Backend only)
cd commerce-pro-backend
docker build -t commerce-pro:latest .
docker run -e GROQ_API_KEY=... -p 8080:8080 commerce-pro:latest
```

---

## 🔍 Codebase Search Tips

- **All REST endpoints**: `@RestController` in backend
- **All entities**: Files with `@Entity` in `src/main/java/com/commerce_pro_backend/*/entity/`
- **All DTOs**: `*DTO.java` or `*Request.java` / `*Response.java`
- **All mappers**: `*Mapper.java` (MapStruct interfaces)
- **All AI features**: `src/main/java/com/commerce_pro_backend/ai/features/`
- **All routes**: `app.routes.ts` in Angular
- **All components**: `*.component.ts` in `src/app/features/` and `src/app/shared/`

---

## ⚠️ Gotchas & Anti-Patterns

1. **DDL Reset**: Schema is recreated on every startup (`create-drop`). **Do NOT add production data directly to DB**; use seed scripts instead.

2. **Lazy Loading**: Hibernate lazy-loads collections by default. **Always use `@Transactional` on service methods** or explicit `Hibernate.initialize()` to avoid LazyInitializationException.

3. **JWT Expiry**: Tokens expire based on `JWT_SECRET` configuration. **Frontend must handle 401 responses** and redirect to login.

4. **CORS on Frontend**: Request headers must match backend's `cors.allowed-origins`. **Local dev must use `http://localhost:4200`**, not `127.0.0.1:4200` if that's not allowed.

5. **AI Budget**: Daily spend capped at `$50.00` (configurable). **All AI requests are rate-limited per-feature**; check `AiRateLimiter` before adding new features.

6. **Groq API Key**: Must be valid and provisioned at `https://console.groq.com`. **Requests fail silently if key is missing**; always check `application.properties` before debugging.

7. **Angular Routes**: `loadChildren` requires a string path in route definitions; do NOT use dynamic imports for lazy-loaded routes (they won't work in production builds).

8. **MapStruct Unmapped Targets**: Configured to `IGNORE` globally. **Ensure all DTO fields are intentionally ignored** or mapped; typos won't error at compile time.

---

## 📞 Support & Further Reading

- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **Angular Docs**: https://angular.dev
- **Groq API**: https://console.groq.com/docs
- **Spring AI**: https://spring.io/projects/spring-ai
- **Render Deployment**: https://render.com/docs/blueprint-spec
- **Tailwind CSS**: https://tailwindcss.com

