package com.commerce_pro_backend.ai.service;

import com.commerce_pro_backend.ai.entity.AiConversation;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.ai.enums.SessionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SupportChatbotService — a conversational AI assistant that answers customer
 * support questions in a multi-turn session.
 *
 * <h3>Architecture</h3>
 * This service is a thin wrapper around {@link AiOrchestrator#chat(AiFeatureType,
 * String, String, String, String, SessionType)}.  The orchestrator handles:
 * <ul>
 *   <li>Feature enable/disable checks</li>
 *   <li>Rate-limiting and budget enforcement</li>
 *   <li>Session creation / resumption via {@link AiConversation}</li>
 *   <li>Maintaining full message history so the model has context for each turn</li>
 *   <li>Usage logging</li>
 * </ul>
 *
 * <h3>Session lifecycle</h3>
 * <pre>
 *   Turn 1:  sessionId = null  → orchestrator creates a new AiConversation
 *            response.sessionId = "session-uuid-abc"
 *
 *   Turn 2:  sessionId = "session-uuid-abc"
 *            orchestrator resumes the session and appends turn 2 to the history
 *            response.sessionId = "session-uuid-abc"  (same)
 *
 *   Turn N:  same pattern; session expires after chatbotTtlHours (default 24 h)
 * </pre>
 *
 * <h3>Persona — the system prompt</h3>
 * The system prompt defines how the chatbot presents itself.  Key rules:
 * <ul>
 *   <li>Always be friendly and empathetic — customers contacting support are often frustrated.
 *   <li>Escalate to a human agent for billing disputes, complaints, or anything
 *       that requires account access.
 *   <li>Never invent policies.  If unsure, say "Let me connect you with a team member."
 *   <li>Keep replies concise — most customers read on mobile.
 * </ul>
 *
 * <h3>Example conversation</h3>
 * <pre>
 *   Customer: "Where is my order #1234?"
 *   Bot:      "Hi! I'm here to help. I don't have direct access to your order
 *              details right now, but you can track order #1234 on our
 *              Order Tracking page. Is there anything else I can help with?"
 *
 *   Customer: "It says 'processing' for 5 days. Is that normal?"
 *   Bot:      "That's longer than usual — our typical processing time is 1-2 business
 *              days. I'd recommend reaching out to our fulfilment team at
 *              support@store.com with your order number. Would you like me to
 *              escalate this for you?"
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SupportChatbotService {

    private final AiOrchestrator orchestrator;

    /** Feature constant — controls model, rate limits, and budget for the chatbot. */
    private static final AiFeatureType FEATURE = AiFeatureType.SUPPORT_CHATBOT;

    /**
     * System prompt that defines the chatbot's persona, capabilities, and limitations.
     *
     * <p>The prompt is sent as the first message of every conversation.
     * The orchestrator inserts it as a SystemMessage before all turn history,
     * so the model stays "in character" across all turns.
     *
     * <p>Escape hatch instruction ("ESCALATE:") is a convention we use to detect
     * when the model decides it cannot help — the controller can then mark the
     * conversation as ESCALATED and route to a human queue.
     */
    private static final String SYSTEM_PROMPT = """
            You are a friendly and helpful customer support assistant for CommercePro store.

            Your responsibilities:
            - Answer questions about orders, products, shipping, and returns.
            - Provide tracking information when available.
            - Guide customers through standard policies.

            Your limitations — do NOT attempt to:
            - Access or modify account data directly.
            - Process refunds, cancellations, or billing changes (escalate instead).
            - Make promises about specific delivery dates unless they are in the context.

            Escalation rule: if the issue requires account access or is a billing dispute,
            start your reply with "ESCALATE:" followed by a brief explanation.
            Example: "ESCALATE: Customer is disputing a charge on order #5678."

            Tone: warm, empathetic, concise — max 3–4 sentences per reply.
            """;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Process a single customer message and return the updated conversation.
     *
     * <p>The returned {@link AiConversation} contains the full session history
     * including the new turn.  The caller should extract the last assistant message
     * using {@code conversation.getTurnsAsList().getLast().assistantMessage()}.
     *
     * @param customerId  UUID of the customer who owns this session
     * @param message     the customer's message for this turn
     * @param sessionId   existing session UUID, or {@code null} to start a new session
     * @return updated {@link AiConversation} with the new turn appended
     *
     * <p>Example — starting a new session (sessionId = null):
     * <pre>
     *   AiConversation session = chatbotService.chat("cust-abc", "Where is my order?", null);
     *   // session.getId()          → "session-uuid-001"  (new session created)
     *   // session.getTurnCount()   → 1
     *   // last assistant message   → "Hi! I'm here to help..."
     * </pre>
     *
     * <p>Example — continuing an existing session:
     * <pre>
     *   AiConversation session = chatbotService.chat("cust-abc",
     *       "It has been 5 days, is that normal?", "session-uuid-001");
     *   // session.getId()        → "session-uuid-001"  (same session)
     *   // session.getTurnCount() → 2
     * </pre>
     */
    public AiConversation chat(String customerId, String message, String sessionId) {
        log.debug("Support chatbot message — customer={}, sessionId={}", customerId, sessionId);

        /*
         * Delegate entirely to the orchestrator.
         * It handles:
         *   1. Feature enabled check (throws AiFeatureDisabledException if toggled off)
         *   2. Rate-limit check (throws AiRateLimitException if exceeded)
         *   3. Budget check (throws AiBudgetExceededException if daily cap hit)
         *   4. Session creation or resumption
         *   5. Building the full message history for multi-turn context
         *   6. Calling Groq and appending the new turn to the conversation
         *   7. Persisting the updated AiConversation + AiUsageLog
         */
        return orchestrator.chat(
                FEATURE,
                SYSTEM_PROMPT,
                message,
                sessionId,
                customerId,
                SessionType.SUPPORT_CHAT);
    }

    /**
     * Check whether a conversation turn was flagged for escalation.
     *
     * <p>The system prompt instructs the model to prefix escalation replies with "ESCALATE:".
     * This helper lets the controller detect that signal and mark the conversation accordingly.
     *
     * <p>Example:
     * <pre>
     *   String lastReply = conversation.getTurnsAsList().getLast().assistantMessage();
     *   if (chatbotService.isEscalationRequired(lastReply)) {
     *       conversation.setStatus("ESCALATED");  // route to human queue
     *   }
     * </pre>
     *
     * @param assistantMessage  the model's reply for the current turn
     * @return {@code true} if the message starts with "ESCALATE:"
     */
    public boolean isEscalationRequired(String assistantMessage) {
        return assistantMessage != null && assistantMessage.startsWith("ESCALATE:");
    }
}
