package com.commerce_pro_backend.ai.service;

import com.commerce_pro_backend.ai.config.AiModuleConfig;
import com.commerce_pro_backend.ai.entity.AiConfig;
import com.commerce_pro_backend.ai.entity.AiConversation;
import com.commerce_pro_backend.ai.entity.AiInsight;
import com.commerce_pro_backend.ai.entity.AiUsageLog;
import com.commerce_pro_backend.ai.enums.AiFeatureType;
import com.commerce_pro_backend.ai.enums.SessionType;
import com.commerce_pro_backend.ai.exception.AiFeatureDisabledException;
import com.commerce_pro_backend.ai.exception.AiResponseParseException;
import com.commerce_pro_backend.ai.exception.ConversationExpiredException;
import com.commerce_pro_backend.ai.repository.AiConfigRepository;
import com.commerce_pro_backend.ai.repository.AiConversationRepository;
import com.commerce_pro_backend.ai.repository.AiInsightRepository;
import com.commerce_pro_backend.ai.repository.AiUsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AiOrchestrator — single entry-point for every AI call in the application.
 *
 * <p>All feature-specific services (fraud, churn, chatbot, …) delegate here.
 * The orchestrator enforces the full pre-call / post-call pipeline:
 *
 * <ol>
 *   <li>Assert feature is enabled         → {@link AiFeatureDisabledException}
 *   <li>Sliding-window rate-limit check   → {@link com.commerce_pro_backend.ai.exception.AiRateLimitException}
 *   <li>Daily budget pre-check            → {@link com.commerce_pro_backend.ai.exception.AiBudgetExceededException}
 *   <li>Call Groq via Spring AI           → {@link AiResponseParseException}
 *   <li>Persist {@link AiUsageLog}
 *   <li>Update {@link AiCostGuard} counters
 *   <li>Return the model's text content
 * </ol>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiOrchestrator {

    private final ChatClient chatClient;
    private final AiConfigRepository configRepo;
    private final AiUsageLogRepository usageLogRepo;
    private final AiInsightRepository insightRepo;
    private final AiConversationRepository conversationRepo;
    private final AiCostGuard costGuard;
    private final AiRateLimiter rateLimiter;
    private final AiModuleConfig moduleConfig;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Single-shot completion — no conversation history.
     * Use for all batch/scored features (fraud, churn, sentiment, …).
     *
     * @param feature      the calling feature
     * @param systemPrompt role/context instructions for the model
     * @param userMessage  the actual payload to analyse
     * @return the model's plain-text response
     */
    public String complete(AiFeatureType feature, String systemPrompt, String userMessage) {
        AiConfig config = loadAndValidateConfig(feature);
        rateLimiter.checkAndIncrement(feature);
        costGuard.checkDailyBudget(feature);

        long start = System.currentTimeMillis();
        ChatResponse response;
        try {
            response = callWithRetry(config, systemPrompt, List.of(new UserMessage(userMessage)));
        } catch (Exception ex) {
            persistFailedLog(feature, config, System.currentTimeMillis() - start, ex);
            throw ex;
        }
        long latency = System.currentTimeMillis() - start;

        String content = extractContent(response, feature);
        persistUsageLog(feature, config, response, latency, true, null, null, null);

        return content;
    }

    /**
     * Single-shot completion that also persists an {@link AiInsight}.
     * Use for features that score an entity (fraud detection, churn prediction, …).
     *
     * @param feature      the calling feature
     * @param systemPrompt role/context instructions for the model
     * @param userMessage  the actual payload to analyse
     * @param entityId     entity being evaluated (transactionId, customerId, …)
     * @param entityType   TRANSACTION | CUSTOMER | PRODUCT | ORDER | …
     * @return the model's plain-text response
     */
    @Transactional
    public String completeAndPersistInsight(
            AiFeatureType feature,
            String systemPrompt,
            String userMessage,
            String entityId,
            String entityType) {

        AiConfig config = loadAndValidateConfig(feature);
        rateLimiter.checkAndIncrement(feature);
        costGuard.checkDailyBudget(feature);

        long start = System.currentTimeMillis();
        ChatResponse response;
        try {
            response = callWithRetry(config, systemPrompt, List.of(new UserMessage(userMessage)));
        } catch (Exception ex) {
            persistFailedLog(feature, config, System.currentTimeMillis() - start, ex);
            throw ex;
        }
        long latency = System.currentTimeMillis() - start;

        String content = extractContent(response, feature);
        AiInsight insight = buildInsight(feature, config, response, content, entityId, entityType, latency);
        insightRepo.save(insight);

        persistUsageLog(feature, config, response, latency, true, null, insight.getId(), null);

        return content;
    }

    /**
     * Multi-turn chat — loads / creates an {@link AiConversation}, appends the new
     * turn, calls Groq with the full history, and persists the updated session.
     *
     * @param feature      SUPPORT_CHATBOT or NL_REPORT
     * @param systemPrompt role instructions (e.g. the chatbot persona)
     * @param userMessage  what the user typed this turn
     * @param sessionId    existing session ID, or null to start a new session
     * @param ownerId      customerId (SUPPORT_CHAT) or userId (NL_REPORT)
     * @param sessionType  SUPPORT_CHAT | NL_REPORT
     * @return updated {@link AiConversation} with the assistant's reply already appended
     */
    @Transactional
    public AiConversation chat(
            AiFeatureType feature,
            String systemPrompt,
            String userMessage,
            String sessionId,
            String ownerId,
            SessionType sessionType) {

        AiConfig config = loadAndValidateConfig(feature);
        rateLimiter.checkAndIncrement(feature);
        costGuard.checkDailyBudget(feature);

        AiConversation conversation = resolveConversation(sessionId, ownerId, sessionType);

        List<Message> history = buildHistory(conversation, userMessage, systemPrompt);

        long start = System.currentTimeMillis();
        ChatResponse response;
        try {
            response = callWithRetry(config, null /* system already in history */, history);
        } catch (Exception ex) {
            persistFailedLog(feature, config, System.currentTimeMillis() - start, ex);
            throw ex;
        }
        long latency = System.currentTimeMillis() - start;

        String assistantReply = extractContent(response, feature);
        conversation.addTurn(userMessage, assistantReply);
        conversation = conversationRepo.save(conversation);

        persistUsageLog(feature, config, response, latency, true, conversation.getId(), null, null);

        return conversation;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private AiConfig loadAndValidateConfig(AiFeatureType feature) {
        AiConfig config = configRepo.findByFeatureType(feature)
                .orElseThrow(() -> new IllegalStateException("No AiConfig row for feature " + feature));
        if (Boolean.FALSE.equals(config.getEnabled())) {
            throw new AiFeatureDisabledException(feature + " is currently disabled");
        }
        return config;
    }

    /**
     * Calls the AI model with exponential-backoff retry.
     * Uses the {@link com.commerce_pro_backend.ai.config.AiModuleConfig.RetryConfig} settings
     * (maxAttempts, backoffMs) configured in application.properties.
     */
    private ChatResponse callWithRetry(AiConfig config, String systemPrompt, List<Message> messages) {
        int maxAttempts = moduleConfig.getRetry().getMaxAttempts();
        long backoffMs  = moduleConfig.getRetry().getBackoffMs();

        Exception lastEx = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return callModel(config, systemPrompt, messages);
            } catch (Exception ex) {
                lastEx = ex;
                if (attempt < maxAttempts) {
                    long delay = backoffMs * (1L << (attempt - 1)); // 1x, 2x, 4x …
                    log.warn("AI call failed (attempt {}/{}), retrying in {}ms: {}",
                            attempt, maxAttempts, delay, ex.getMessage());
                    try { Thread.sleep(delay); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during AI retry backoff", ie);
                    }
                }
            }
        }
        log.error("AI call failed after {} attempts", maxAttempts);
        throw new RuntimeException("AI call failed after " + maxAttempts + " attempts", lastEx);
    }

    /**
     * Single attempt to call the AI model via Spring AI ChatClient.
     * Correctly passes the system prompt when provided (single-turn), or uses
     * the messages list directly when it already contains a SystemMessage (multi-turn).
     */
    private ChatResponse callModel(AiConfig config, String systemPrompt, List<Message> messages) {
        int maxTokens = Math.min(config.getMaxTokens(), moduleConfig.getMaxTokensCap());

        var options = org.springframework.ai.openai.OpenAiChatOptions.builder()
                .model(config.getModel())
                .maxTokens(maxTokens)
                .build();

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            // Single-turn: pass system prompt via the fluent API so it is correctly set
            return chatClient.prompt()
                    .system(systemPrompt)
                    .messages(messages)
                    .options(options)
                    .call()
                    .chatResponse();
        }

        // Multi-turn: the messages list already contains a SystemMessage as the first element
        return chatClient.prompt(new Prompt(messages))
                .options(options)
                .call()
                .chatResponse();
    }

    private String extractContent(ChatResponse response, AiFeatureType feature) {
        try {
            String content = response.getResult().getOutput().getText();
            if (content == null || content.isBlank()) {
                throw new AiResponseParseException(feature + ": model returned an empty response");
            }
            return content;
        } catch (AiResponseParseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiResponseParseException(feature + ": failed to extract content from response", ex);
        }
    }

    private AiConversation resolveConversation(String sessionId, String ownerId, SessionType sessionType) {
        if (sessionId != null) {
            AiConversation existing = conversationRepo.findById(sessionId)
                    .orElseThrow(() -> new ConversationExpiredException("Session " + sessionId + " not found"));
            if (existing.isExpired()) {
                throw new ConversationExpiredException("Session " + sessionId + " has expired");
            }
            return existing;
        }

        int ttlHours = sessionType == SessionType.SUPPORT_CHAT
                ? moduleConfig.getSession().getChatbotTtlHours()
                : moduleConfig.getSession().getReportTtlHours();

        AiConversation fresh = AiConversation.builder()
                .sessionType(sessionType)
                .customerId(sessionType == SessionType.SUPPORT_CHAT ? ownerId : null)
                .userId(sessionType == SessionType.NL_REPORT ? ownerId : null)
                .expiresAt(LocalDateTime.now().plusHours(ttlHours))
                .status("ACTIVE")
                .build();

        return conversationRepo.save(fresh);
    }

    /**
     * Build the full message list: [system, ...history pairs, new user turn].
     */
    private List<Message> buildHistory(AiConversation conversation, String userMessage, String systemPrompt) {
        List<Message> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new org.springframework.ai.chat.messages.SystemMessage(systemPrompt));
        }
        conversation.getTurnsAsList().forEach(turn -> {
            messages.add(new UserMessage(turn.userMessage()));
            messages.add(new AssistantMessage(turn.assistantMessage()));
        });
        messages.add(new UserMessage(userMessage));
        return messages;
    }

    private void persistUsageLog(
            AiFeatureType feature,
            AiConfig config,
            ChatResponse response,
            long latencyMs,
            boolean success,
            String conversationId,
            String insightId,
            String errorType) {

        int inputTokens  = 0;
        int outputTokens = 0;
        try {
            var usage = response.getMetadata().getUsage();
            inputTokens  = usage.getPromptTokens() != null  ? usage.getPromptTokens().intValue()  : 0;
            outputTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens().intValue() : 0;
        } catch (Exception ignored) { /* token counts are best-effort */ }

        CostCalculator.CostBreakdown cost =
                CostCalculator.breakdown(config.getModel(), inputTokens, outputTokens);

        costGuard.recordSpend(feature, cost.totalCostUsd());

        usageLogRepo.save(AiUsageLog.builder()
                .featureType(feature)
                .model(config.getModel())
                .conversationId(conversationId)
                .insightId(insightId)
                .tokensInput(inputTokens)
                .tokensOutput(outputTokens)
                .costInputUsd(cost.inputCostUsd())
                .costOutputUsd(cost.outputCostUsd())
                .totalCostUsd(cost.totalCostUsd())
                .latencyMs(latencyMs)
                .calledAt(LocalDateTime.now())
                .success(success)
                .errorType(errorType)
                .build());
    }

    private void persistFailedLog(AiFeatureType feature, AiConfig config, long latencyMs, Exception ex) {
        usageLogRepo.save(AiUsageLog.builder()
                .featureType(feature)
                .model(config.getModel())
                .latencyMs(latencyMs)
                .calledAt(LocalDateTime.now())
                .success(false)
                .errorType(ex.getClass().getSimpleName())
                .errorMessage(ex.getMessage() != null
                        ? ex.getMessage().substring(0, Math.min(500, ex.getMessage().length()))
                        : null)
                .build());
    }

    private AiInsight buildInsight(
            AiFeatureType feature,
            AiConfig config,
            ChatResponse response,
            String content,
            String entityId,
            String entityType,
            long latencyMs) {

        int inputTokens  = 0;
        int outputTokens = 0;
        try {
            var usage = response.getMetadata().getUsage();
            inputTokens  = usage.getPromptTokens() != null  ? usage.getPromptTokens().intValue()  : 0;
            outputTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens().intValue() : 0;
        } catch (Exception ignored) {}

        BigDecimal cost = CostCalculator.calculate(config.getModel(), inputTokens, outputTokens);

        return AiInsight.builder()
                .featureType(feature)
                .entityId(entityId)
                .entityType(entityType)
                .reasoning(content)
                .rawOutput(content)
                .modelUsed(config.getModel())
                .tokensInput(inputTokens)
                .tokensOutput(outputTokens)
                .estimatedCostUsd(cost)
                .latencyMs(latencyMs)
                .status("PROCESSED")
                .build();
    }
}
