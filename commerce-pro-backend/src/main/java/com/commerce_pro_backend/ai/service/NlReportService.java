package com.commerce_pro_backend.ai.service;

import com.commerce_pro_backend.ai.entity.AiConversation;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.ai.enums.SessionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * NlReportService — lets admin users query business data in plain English.
 *
 * <h3>What is a Natural Language Report?</h3>
 * Instead of navigating dashboards and filters, an admin can type a question like:
 * <ul>
 *   <li>"What were our top 5 revenue products last month?"</li>
 *   <li>"Which customer segment had the highest churn risk this quarter?"</li>
 *   <li>"Show me the trend in average order value over the last 6 weeks."</li>
 * </ul>
 * The AI interprets the question, provides a plain-English answer with context,
 * and can follow up on clarifications across multiple turns.
 *
 * <h3>Architecture</h3>
 * This service is a thin wrapper around {@link AiOrchestrator#chat()}, identical in
 * structure to {@link SupportChatbotService} but with a different system prompt
 * (analyst persona instead of support persona) and {@link SessionType#NL_REPORT}.
 *
 * <h3>Important limitation</h3>
 * The model does NOT have direct database access — it works from context injected
 * into each turn.  For a richer implementation, callers can pre-fetch relevant
 * aggregates and inject them into the first user message:
 * <pre>
 *   String context = analyticsService.buildContextSummary();
 *   String message = context + "\n\nQuestion: " + userQuestion;
 *   nlReportService.chat(userId, message, null);
 * </pre>
 *
 * <h3>Session TTL</h3>
 * Report sessions expire after {@code ai.session.reportTtlHours} (default 2 h)
 * because they are shorter-lived work sessions, not persistent support conversations.
 *
 * <h3>Example conversation</h3>
 * <pre>
 *   Turn 1 — user: "Our total revenue last month was $48,320, top product was
 *                    Wireless Headphones at $12,000. What drove this?"
 *            bot:  "Based on the numbers you shared, Wireless Headphones contributed
 *                   ~25% of revenue. This concentration suggests strong demand in the
 *                   audio category. Are there any promotions that ran last month?"
 *
 *   Turn 2 — user: "Yes, we ran a 15% off promo for the first 2 weeks."
 *            bot:  "That explains the spike. The promo likely pulled forward demand.
 *                   I'd recommend comparing the 2 weeks after the promo to check
 *                   for a post-promotion dip (demand cannibalization)."
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NlReportService {

    private final AiOrchestrator orchestrator;

    /** Feature constant for rate-limiting and budget enforcement. */
    private static final AiFeatureType FEATURE = AiFeatureType.NL_REPORT;

    /**
     * System prompt defining the AI analyst's persona and scope.
     *
     * <p>Key rules:
     * - The analyst answers questions about the data the user provides.
     * - It never invents numbers — "Based on the data you shared" hedging prevents hallucination.
     * - It asks one clarifying question at a time to stay focused.
     * - It can suggest follow-up analyses to guide the conversation toward actionable insight.
     */
    private static final String SYSTEM_PROMPT = """
            You are a senior business intelligence analyst for an e-commerce company.
            You answer questions about sales, customers, inventory, and finances
            based ONLY on the data the user provides in this conversation.

            Rules:
            - Never invent numbers. If data is missing, say so and ask the user to provide it.
            - When you see a trend, explain the likely business cause.
            - After answering, suggest one relevant follow-up question or analysis.
            - Keep answers concise — 3–5 sentences unless the user asks for detail.
            - Tone: professional, direct, data-driven.
            """;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Process one turn of a natural-language reporting conversation.
     *
     * @param userId     UUID of the admin user (session isolation)
     * @param message    the admin's question or data dump for this turn
     * @param sessionId  existing session UUID, or {@code null} to start a new session
     * @return updated {@link AiConversation} with the analyst's reply appended
     *
     * <p>Example — turn 1 (new session):
     * <pre>
     *   AiConversation c = nlReportService.chat("user-uuid",
     *       "Revenue this month: $48,320. Last month: $41,100. What changed?", null);
     *   // c.getId()        → "sess-uuid-001"
     *   // c.getTurnCount() → 1
     *   // last reply       → "Revenue grew 17.6% month-over-month. ..."
     * </pre>
     *
     * <p>Example — turn 2 (continuing session):
     * <pre>
     *   AiConversation c = nlReportService.chat("user-uuid",
     *       "New customers this month: 320, last month: 280.", "sess-uuid-001");
     *   // c.getTurnCount() → 2
     * </pre>
     */
    public AiConversation chat(String userId, String message, String sessionId) {
        log.debug("NL report chat — user={}, session={}", userId, sessionId);

        return orchestrator.chat(
                FEATURE,
                SYSTEM_PROMPT,
                message,
                sessionId,
                userId,
                SessionType.NL_REPORT);   // NL_REPORT sessions expire after reportTtlHours (default 2 h)
    }
}
