package com.commerce_pro_backend.ai.controller;

import com.commerce_pro_backend.ai.dto.request.AiChatRequest;
import com.commerce_pro_backend.ai.dto.response.AiChatResponse;
import com.commerce_pro_backend.ai.entity.AiConversation;
import com.commerce_pro_backend.ai.repository.AiConversationRepository;
import com.commerce_pro_backend.ai.service.SupportChatbotService;
import com.commerce_pro_backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * AiChatbotController — REST interface for the AI-powered customer support chatbot.
 *
 * <h3>Base path</h3>
 * {@code /api/v1/ai/chatbot}
 *
 * <h3>Multi-turn session model</h3>
 * Sessions are maintained server-side in {@link AiConversation}.
 * The client only needs to store the {@code sessionId} returned in the first reply
 * and include it in all subsequent requests.
 *
 * <pre>
 *   POST /api/v1/ai/chatbot/chat
 *   Body: { "message": "Where is my order?", "sessionId": null }
 *   → Response: { "sessionId": "sess-001", "message": "Hi! I can help...", "turnCount": 1 }
 *
 *   POST /api/v1/ai/chatbot/chat
 *   Body: { "message": "It's been 5 days", "sessionId": "sess-001" }
 *   → Response: { "sessionId": "sess-001", "message": "That's longer than usual...", "turnCount": 2 }
 * </pre>
 *
 * <h3>Escalation handling</h3>
 * When the model decides it cannot help (billing disputes, account access, etc.),
 * it prefixes the reply with "ESCALATE:".  This controller detects that, marks
 * the conversation status as "ESCALATED", and strips the prefix before returning
 * the reply to the client.
 *
 * <h3>Security</h3>
 * The {@code /chat} endpoint is accessible to any authenticated user (customers
 * and admins both).  Session isolation is enforced by tying each session to
 * the authenticated user's ID.
 */
@RestController
@RequestMapping("/api/v1/ai/chatbot")
@RequiredArgsConstructor
@Slf4j
public class AiChatbotController {

    private final SupportChatbotService      chatbotService;
    private final AiConversationRepository   conversationRepo;

    /**
     * POST /api/v1/ai/chatbot/chat
     *
     * <p>Processes one customer message and returns the bot's reply.
     * Pass {@code sessionId: null} (or omit it) to start a new session.
     * Include the {@code sessionId} from the previous response to continue a session.
     *
     * <h3>Example — first message (new session)</h3>
     * <pre>
     * Request:
     * {
     *   "message": "Hi, where is my order #1234?",
     *   "sessionId": null
     * }
     *
     * Response (200 OK):
     * {
     *   "success": true,
     *   "data": {
     *     "sessionId": "session-uuid-001",
     *     "message": "Hi there! I'm here to help. I don't have direct access to order
     *                 details, but you can track order #1234 on our Order Tracking page.
     *                 Is there anything else I can help with?",
     *     "turnCount": 1
     *   }
     * }
     * </pre>
     *
     * <h3>Example — escalated reply</h3>
     * <pre>
     * Request:  { "message": "I was charged twice!", "sessionId": "session-uuid-001" }
     * Response:
     * {
     *   "success": true,
     *   "data": {
     *     "sessionId": "session-uuid-001",
     *     "message": "I understand that's very frustrating. I'm connecting you with our
     *                  billing team now — they'll get this sorted for you right away.",
     *     "turnCount": 2
     *   }
     * }
     * </pre>
     * (The conversation status is also set to ESCALATED internally)
     *
     * @param request    the chat request containing the message and optional sessionId
     * @param principal  the authenticated user (customer or admin)
     */
    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('ai:chatbot:use')")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @RequestBody @Valid AiChatRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        // Use the authenticated user's username as the customer / owner ID for session isolation.
        // In a real implementation this would be the customer UUID from the JWT claims.
        String ownerId = principal.getUsername();

        // Delegate to the service — returns the updated AiConversation with the new turn
        AiConversation conversation = chatbotService.chat(
                ownerId,
                request.message(),
                request.sessionId());

        // Extract the last assistant message from the conversation history
        // The conversation.getTurnsAsList() list is ordered oldest-first, so getLast()
        // gives us the reply that was just generated.
        var turns = conversation.getTurnsAsList();
        String assistantReply = turns.isEmpty() ? "" : turns.get(turns.size() - 1).assistantMessage();

        // Detect escalation: the system prompt instructs the model to start
        // escalation replies with "ESCALATE:" followed by the message.
        if (chatbotService.isEscalationRequired(assistantReply)) {
            // Mark the conversation as escalated for the human support queue
            conversation.setStatus("ESCALATED");
            conversation.setEscalatedAt(java.time.LocalDateTime.now());
            conversationRepo.save(conversation);

            // Strip the "ESCALATE: " prefix before sending the reply to the customer
            // so they see a friendly handoff message, not a protocol signal.
            assistantReply = assistantReply.substring("ESCALATE:".length()).trim();

            log.info("Chatbot session {} escalated to human agent for user {}",
                    conversation.getId(), ownerId);
        }

        AiChatResponse response = new AiChatResponse(
                conversation.getId(),
                assistantReply,
                conversation.getTurnCount()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/ai/chatbot/sessions/{sessionId}
     *
     * <p>Returns the full conversation history for a session.
     * Useful for the admin support dashboard to review what the bot told a customer.
     *
     * <h3>Example response</h3>
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "id": "session-uuid-001",
     *     "status": "ACTIVE",
     *     "turnCount": 3,
     *     "turnsJson": "[{\"userMessage\":\"Hi\",\"assistantMessage\":\"Hello!\",\"timestamp\":\"...\"}]"
     *   }
     * }
     * </pre>
     *
     * @param sessionId  UUID of the AiConversation session
     */
    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('ai:admin')")
    public ResponseEntity<ApiResponse<AiConversation>> getSession(
            @PathVariable String sessionId) {

        AiConversation session = conversationRepo.findById(sessionId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Session not found: " + sessionId));

        return ResponseEntity.ok(ApiResponse.success(session));
    }
}
