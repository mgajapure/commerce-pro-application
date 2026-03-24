package com.commerce_pro_backend.ai.controller;

import com.commerce_pro_backend.ai.config.AiModuleConfig;
import com.commerce_pro_backend.ai.dto.request.AiConfigUpdateRequest;
import com.commerce_pro_backend.ai.dto.response.AiConfigResponse;
import com.commerce_pro_backend.ai.dto.response.AiDashboardResponse;
import com.commerce_pro_backend.ai.dto.response.AiUsageSummaryItem;
import com.commerce_pro_backend.ai.entity.AiConfig;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.ai.repository.AiConfigRepository;
import com.commerce_pro_backend.ai.repository.AiUsageLogRepository;
import com.commerce_pro_backend.ai.service.AiCostGuard;
import com.commerce_pro_backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

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
    private final AiCostGuard costGuard;
    private final AiModuleConfig moduleConfig;

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
