package com.commerce_pro_backend.ai.service;

import com.commerce_pro_backend.ai.entity.AiInsight;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.ai.repository.AiInsightRepository;
import com.commerce_pro_backend.finance.entity.Budget;
import com.commerce_pro_backend.finance.repository.BudgetRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * BudgetAnomalyService — uses AI to detect unusual spending patterns in a budget
 * by comparing budgeted vs actual amounts across budget lines.
 *
 * <h3>What counts as an anomaly?</h3>
 * <ul>
 *   <li><b>Overspend</b>       — actual &gt; budgeted × 1.10 (more than 10% over)</li>
 *   <li><b>Underspend</b>      — actual &lt; budgeted × 0.50 (less than 50% spent,
 *       suggesting the budget was padded or the activity didn't happen)</li>
 *   <li><b>Zero actual</b>     — budgeted amount &gt; 0 but actual = 0
 *       (activity may have been cancelled or not tracked)</li>
 *   <li><b>Unusual pattern</b> — spending concentrated in one period that differs
 *       from historical norms</li>
 * </ul>
 *
 * <h3>Example prompt payload (summarised BudgetLine variances)</h3>
 * <pre>
 * {
 *   "budgetId":   "bgt_2025_q1",
 *   "fiscalYear":  2025,
 *   "period":     "Q1",
 *   "lines": [
 *     {"name":"Marketing","budgeted":20000,"actual":31500,"variancePct":57.5},
 *     {"name":"Operations","budgeted":15000,"actual":14800,"variancePct":-1.3},
 *     {"name":"R&D","budgeted":10000,"actual":800,"variancePct":-92.0}
 *   ]
 * }
 * </pre>
 *
 * <h3>Example model response → {@link AnomalyResult}</h3>
 * <pre>
 * {
 *   "anomalyScore":    78,
 *   "severity":       "HIGH",
 *   "anomalousLines": ["Marketing","R&D"],
 *   "anomalyTypes":   ["overspend","underspend"],
 *   "recommendation": "INVESTIGATE",
 *   "reasoning":      "Marketing is 57.5% over budget — requires immediate explanation.
 *                      R&D is 92% under budget, indicating planned activities may not
 *                      have occurred. Both lines need CFO review."
 * }
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BudgetAnomalyService {

    private final AiOrchestrator      orchestrator;
    private final AiInsightRepository insightRepo;
    private final BudgetRepository    budgetRepo;

    private static final AiFeatureType FEATURE = AiFeatureType.BUDGET_ANOMALY;

    private static final String SYSTEM_PROMPT = """
            You are a senior financial controller specialising in budget variance analysis.
            Review the budget vs actual data and identify anomalous spending patterns.

            You MUST respond with ONLY a JSON object in this exact schema — no markdown, no explanation:
            {
              "anomalyScore":    <integer 0-100, where 0=no anomalies, 100=severe anomalies>,
              "severity":        <"LOW" | "MEDIUM" | "HIGH" | "CRITICAL">,
              "anomalousLines":  [<names of budget lines with anomalies>],
              "anomalyTypes":    [<types found: "overspend" | "underspend" | "zero_actual" | "unusual_pattern">],
              "recommendation":  <"APPROVE" | "MONITOR" | "INVESTIGATE" | "ESCALATE_TO_CFO">,
              "reasoning":       <plain-English explanation, 2-4 sentences>
            }
            """;

    /**
     * Detect budget anomalies for a specific budget record.
     *
     * @param budgetId  UUID of the {@link Budget} to analyse
     * @return populated {@link AnomalyResult}
     *
     * <p>Example:
     * <pre>
     *   AnomalyResult r = budgetAnomalyService.detect("bgt-uuid");
     *   // r.anomalyScore()    → 78
     *   // r.severity()        → "HIGH"
     *   // r.anomalousLines()  → ["Marketing", "R&D"]
     *   // r.recommendation()  → "INVESTIGATE"
     * </pre>
     */
    @Transactional
    public AnomalyResult detect(String budgetId) {
        log.info("Budget anomaly detection requested for budget: {}", budgetId);

        Budget budget = budgetRepo.findById(budgetId)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found: " + budgetId));

        // Serialise each budget line's variance data as a JSON array element.
        // variancePct: positive = overspend, negative = underspend.
        // Example: {"name":"Marketing","budgeted":20000,"actual":31500,"variancePct":57.5}
        String linesJson = budget.getLines() == null ? "[]" : budget.getLines().stream()
                .map(line -> String.format(
                        "{\"name\":\"%s\",\"budgeted\":%s,\"actual\":%s,\"variancePct\":%s}",
                        nvl(line.getLineName()),
                        nvlDec(line.getBudgetedAmount()),
                        nvlDec(line.getActualAmount()),
                        nvlDec(line.getVariancePct())))
                .collect(Collectors.joining(",\n    ", "[\n    ", "\n  ]"));

        String userMessage = """
                {
                  "budgetId":   "%s",
                  "fiscalYear":  %d,
                  "status":     "%s",
                  "totalRevenueBudget": %s,
                  "totalExpenseBudget": %s,
                  "lines": %s
                }
                """.formatted(
                budget.getId(),
                nvlInt(budget.getFiscalYear()),
                nvl(budget.getStatus()),
                nvlDec(budget.getTotalRevenueBudget()),
                nvlDec(budget.getTotalExpenseBudget()),
                linesJson);

        String rawResponse = orchestrator.complete(FEATURE, SYSTEM_PROMPT, userMessage);
        AnomalyResult result = PromptJsonExtractor.parse(rawResponse, AnomalyResult.class, FEATURE.name());

        insightRepo.save(AiInsight.builder()
                .featureType(FEATURE)
                .entityId(budgetId)
                .entityType("BUDGET")
                .score(result.anomalyScore())
                .riskLevel(result.severity())
                .recommendation(result.recommendation())
                .signals(result.anomalyTypes() != null ? String.join(",", result.anomalyTypes()) : "")
                .reasoning(result.reasoning())
                .rawOutput(result.toString())
                .status("PROCESSED")
                .build());

        return result;
    }

    private String nvl(Object o)                     { return o != null ? o.toString() : ""; }
    private int    nvlInt(Integer i)                  { return i != null ? i : 0; }
    private String nvlDec(java.math.BigDecimal d)     { return d != null ? d.toPlainString() : "0.00"; }

    /** AnomalyResult — the model's budget anomaly assessment. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AnomalyResult(
        /** 0 = healthy budget, 100 = critical anomalies. Maps to AiInsight.score. */
        int anomalyScore,
        /** LOW | MEDIUM | HIGH | CRITICAL */
        String severity,
        /** Names of the budget lines that have anomalies, e.g. ["Marketing", "R&D"]. */
        List<String> anomalousLines,
        /** Types detected: overspend | underspend | zero_actual | unusual_pattern. */
        List<String> anomalyTypes,
        /** APPROVE | MONITOR | INVESTIGATE | ESCALATE_TO_CFO */
        String recommendation,
        /** Plain-English explanation for the finance team. */
        String reasoning
    ) {}
}
