package com.commerce_pro_backend.ai.controller;

import com.commerce_pro_backend.ai.dto.request.AiChatRequest;
import com.commerce_pro_backend.ai.dto.response.AiChatResponse;
import com.commerce_pro_backend.ai.entity.AiConversation;
import com.commerce_pro_backend.ai.enums.SessionType;
import com.commerce_pro_backend.ai.repository.AiConversationRepository;
import com.commerce_pro_backend.ai.service.NlReportService;
import com.commerce_pro_backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * AiNlReportController — conversational AI business analytics endpoint.
 *
 * <h3>Base path</h3>
 * {@code /api/v1/ai/nl-report}
 *
 * <h3>Usage pattern</h3>
 * The admin types a business question (optionally including raw data).
 * The AI responds with an analysis and a suggested follow-up question.
 * Subsequent turns continue in the same session.
 *
 * <h3>Example conversation</h3>
 * <pre>
 *   Turn 1: "Revenue this month: $48,320. Last month: $41,100. What changed?"
 *   Turn 2: "New customers this month: 320, last month: 280."
 *   Turn 3: "Show me what actions I should take based on this data."
 * </pre>
 */
@RestController
@RequestMapping("/v1/ai/nl-report")
@RequiredArgsConstructor
public class AiNlReportController {

    private final NlReportService            nlReportService;
    private final AiConversationRepository   conversationRepo;

    /**
     * POST /api/v1/ai/nl-report/chat
     *
     * <p>Ask a business analytics question or share data for analysis.
     * Pass {@code sessionId: null} to start a new session; include the returned
     * {@code sessionId} in subsequent calls to continue the conversation.
     *
     * <h3>Example request — turn 1 (new session)</h3>
     * <pre>
     * {
     *   "message": "Our churn rate last month was 8.2%. Industry average is 5%. What could cause this?",
     *   "sessionId": null
     * }
     * </pre>
     *
     * <h3>Example response (200 OK)</h3>
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "sessionId": "sess-uuid-001",
     *     "message": "An 8.2% churn rate vs 5% industry average is significant.
     *                  Common causes include pricing pressure, poor post-purchase
     *                  experience, or a competitor promotion. Do you have data on
     *                  the return rate or NPS scores for the same period?",
     *     "turnCount": 1
     *   }
     * }
     * </pre>
     */
    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('ai:nl-report:use')")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @RequestBody @Valid AiChatRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        AiConversation session = nlReportService.chat(
                principal.getUsername(),
                request.message(),
                request.sessionId());

        var turns = session.getTurnsAsList();
        String reply = turns.isEmpty() ? "" : turns.get(turns.size() - 1).assistantMessage();

        return ResponseEntity.ok(ApiResponse.success(
                new AiChatResponse(session.getId(), reply, session.getTurnCount())));
    }

    /**
     * GET /api/v1/ai/nl-report/sessions
     * Lists active NL report sessions for the authenticated admin user, newest first.
     */
    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('ai:nl-report:use')")
    public ResponseEntity<ApiResponse<java.util.List<NlSessionSummary>>> listSessions(
            @AuthenticationPrincipal UserDetails principal) {

        var sessions = conversationRepo
                .findByUserIdAndSessionTypeAndStatusOrderByLastActiveAtDesc(
                        principal.getUsername(), SessionType.NL_REPORT, "ACTIVE")
                .stream()
                .map(NlSessionSummary::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    /**
     * GET /api/v1/ai/nl-report/sessions/{sessionId}
     * Returns full turn history for a specific NL report session.
     */
    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('ai:nl-report:use')")
    public ResponseEntity<ApiResponse<NlSessionDetail>> getSession(
            @PathVariable String sessionId) {

        AiConversation conv = conversationRepo.findById(sessionId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Session not found: " + sessionId));

        return ResponseEntity.ok(ApiResponse.success(NlSessionDetail.from(conv)));
    }

    // ── Nested projections ────────────────────────────────────────────────────

    /** Lightweight session summary for the sidebar list. */
    public record NlSessionSummary(
            String id,
            String preview,
            int turnCount,
            java.time.LocalDateTime lastActiveAt,
            java.time.LocalDateTime createdAt
    ) {
        static NlSessionSummary from(AiConversation c) {
            var turns = c.getTurnsAsList();
            String preview = turns.isEmpty() ? "(empty)"
                    : turns.get(0).userMessage().substring(0, Math.min(80, turns.get(0).userMessage().length()));
            return new NlSessionSummary(c.getId(), preview, c.getTurnCount(),
                    c.getLastActiveAt(), c.getCreatedAt());
        }
    }

    /** Full session detail including all turns, for loading a past session. */
    public record NlSessionDetail(
            String id,
            int turnCount,
            java.util.List<TurnDto> turns,
            java.time.LocalDateTime createdAt
    ) {
        static NlSessionDetail from(AiConversation c) {
            var turns = c.getTurnsAsList().stream()
                    .map(t -> new TurnDto(t.userMessage(), t.assistantMessage()))
                    .toList();
            return new NlSessionDetail(c.getId(), c.getTurnCount(), turns, c.getCreatedAt());
        }
    }

    public record TurnDto(String userMessage, String assistantMessage) {}
}
