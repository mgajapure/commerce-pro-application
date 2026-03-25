package com.commerce_pro_backend.ai.controller;

import com.commerce_pro_backend.ai.config.AiModuleConfig;
import com.commerce_pro_backend.ai.dto.request.AiConfigUpdateRequest;
import com.commerce_pro_backend.ai.dto.response.AiConfigResponse;
import com.commerce_pro_backend.ai.dto.response.AiDashboardResponse;
import com.commerce_pro_backend.ai.dto.response.AiInsightResponse;
import com.commerce_pro_backend.ai.dto.response.AiUsageSummaryItem;
import com.commerce_pro_backend.ai.entity.AiConfig;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.ai.repository.AiConfigRepository;
import com.commerce_pro_backend.ai.repository.AiInsightRepository;
import com.commerce_pro_backend.ai.repository.AiUsageLogRepository;
import com.commerce_pro_backend.ai.service.AiCostGuard;
import com.commerce_pro_backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AiAdminController — manages AI feature configuration, cost dashboards, and usage analytics.
 *
 * All endpoints require the ADMIN role.
 */
@RestController
@RequestMapping("/v1/admin/ai")
@RequiredArgsConstructor
public class AiAdminController {

    private final AiConfigRepository configRepo;
    private final AiUsageLogRepository usageLogRepo;
    private final AiInsightRepository insightRepo;
    private final AiCostGuard costGuard;
    private final AiModuleConfig moduleConfig;

    // ── AI Insights browser ───────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/ai/insights
     * Paginated list of AI decisions (insights) with optional filters.
     *
     * Query params:
     *   feature    — one of AiFeatureType enum values (optional)
     *   riskLevel  — LOW | MEDIUM | HIGH | CRITICAL (optional)
     *   entityType — TRANSACTION | CUSTOMER | PRODUCT | … (optional)
     *   since      — ISO-8601 datetime (optional, defaults to last 7 days)
     *   page       — 0-based page index (default 0)
     *   size       — page size (default 20, max 100)
     */
    @GetMapping("/insights")
    @PreAuthorize("hasAuthority('ai:config:manage')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listInsights(
            @RequestParam(required = false) AiFeatureType feature,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int cappedSize = Math.min(size, 100);
        LocalDateTime effectiveSince = since != null ? since : LocalDateTime.now().minusDays(7);

        Page<AiInsightResponse> results = insightRepo
                .findByFilters(feature, riskLevel, entityType, effectiveSince,
                        PageRequest.of(page, cappedSize, Sort.by("createdAt").descending()))
                .map(AiInsightResponse::from);

        Map<String, Object> body = Map.of(
                "content",       results.getContent(),
                "totalElements", results.getTotalElements(),
                "totalPages",    results.getTotalPages(),
                "page",          results.getNumber(),
                "size",          results.getSize()
        );

        return ResponseEntity.ok(ApiResponse.success(body));
    }

    // ── Config management ─────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/ai/configs
     * Returns the runtime configuration for every AI feature.
     */
    @GetMapping("/configs")
    @PreAuthorize("hasAuthority('ai:config:manage')")
    public ResponseEntity<ApiResponse<List<AiConfigResponse>>> listConfigs() {
        List<AiConfigResponse> configs = configRepo.findAll()
                .stream()
                .map(AiConfigResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    /**
     * GET /api/v1/admin/ai/configs/{feature}
     * Returns configuration for a single feature.
     */
    @GetMapping("/configs/{feature}")
    @PreAuthorize("hasAuthority('ai:config:manage')")
    public ResponseEntity<ApiResponse<AiConfigResponse>> getConfig(
            @PathVariable AiFeatureType feature) {

        AiConfig config = configRepo.findByFeatureType(feature)
                .orElseThrow(() -> new IllegalArgumentException("No config for feature: " + feature));
        return ResponseEntity.ok(ApiResponse.success(AiConfigResponse.from(config)));
    }

    /**
     * PATCH /api/v1/admin/ai/configs/{feature}
     * Partially updates a feature's configuration. Only non-null fields are applied.
     */
    @PatchMapping("/configs/{feature}")
    @PreAuthorize("hasAuthority('ai:config:manage')")
    public ResponseEntity<ApiResponse<AiConfigResponse>> updateConfig(
            @PathVariable AiFeatureType feature,
            @RequestBody @Valid AiConfigUpdateRequest req,
            @AuthenticationPrincipal UserDetails principal) {

        AiConfig config = configRepo.findByFeatureType(feature)
                .orElseThrow(() -> new IllegalArgumentException("No config for feature: " + feature));

        if (req.enabled()            != null) config.setEnabled(req.enabled());
        if (req.model()              != null) config.setModel(req.model());
        if (req.maxTokens()          != null) config.setMaxTokens(req.maxTokens());
        if (req.dailyBudgetUsd()     != null) config.setDailyBudgetUsd(req.dailyBudgetUsd());
        if (req.monthlyBudgetUsd()   != null) config.setMonthlyBudgetUsd(req.monthlyBudgetUsd());
        if (req.rateLimitPerMinute() != null) config.setRateLimitPerMinute(req.rateLimitPerMinute());
        if (req.rateLimitPerHour()   != null) config.setRateLimitPerHour(req.rateLimitPerHour());
        if (req.fallbackStrategy()   != null) config.setFallbackStrategy(req.fallbackStrategy());
        if (req.defaultScore()       != null) config.setDefaultScore(req.defaultScore());

        config.setUpdatedBy(principal.getUsername());
        AiConfig saved = configRepo.save(config);

        return ResponseEntity.ok(ApiResponse.success("Config updated", AiConfigResponse.from(saved)));
    }

    // ── Usage analytics ───────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/ai/usage?from=...&to=...
     * Cost and token breakdown by feature for the requested time window.
     * Defaults to the last 30 days when parameters are omitted.
     */
    @GetMapping("/usage")
    @PreAuthorize("hasAuthority('ai:config:manage')")
    public ResponseEntity<ApiResponse<List<AiUsageSummaryItem>>> usageSummary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to) {

        LocalDateTime effectiveTo   = to   != null ? to   : LocalDateTime.now();
        LocalDateTime effectiveFrom = from != null ? from : effectiveTo.minusDays(30);

        List<AiUsageSummaryItem> summary = usageLogRepo
                .costSummaryByFeature(effectiveFrom, effectiveTo)
                .stream()
                .map(AiUsageSummaryItem::fromRow)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // ── Live dashboard ────────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/ai/dashboard
     * Real-time spend counters, call success/failure rates, and slow-call counts.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('ai:config:manage')")
    public ResponseEntity<ApiResponse<AiDashboardResponse>> dashboard() {
        LocalDateTime since24h = LocalDateTime.now().minusHours(24);

        long successfulCalls = usageLogRepo.countBySuccessAndCalledAtAfter(true,  since24h);
        long failedCalls     = usageLogRepo.countBySuccessAndCalledAtAfter(false, since24h);
        long slowCalls       = usageLogRepo
                .findByLatencyMsGreaterThanAndCalledAtAfter(3_000L, since24h).size();

        AiDashboardResponse dash = new AiDashboardResponse(
                costGuard.getGlobalDailySpend(),
                moduleConfig.getBudget().getDailyTotalUsd(),
                costGuard.getDailySpendSummary(),
                successfulCalls,
                failedCalls,
                slowCalls
        );

        return ResponseEntity.ok(ApiResponse.success(dash));
    }
}
