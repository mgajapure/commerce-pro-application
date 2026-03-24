package com.commerce_pro_backend.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for a single conversational turn.
 * Used by the support chatbot and NL-report chat endpoints.
 */
public record AiChatRequest(

    /** The user's message for this turn. */
    @NotBlank
    @Size(max = 4000)
    String message,

    /**
     * Optional session ID to continue an existing conversation.
     * Pass null / omit to start a new session.
     */
    String sessionId
) {}
