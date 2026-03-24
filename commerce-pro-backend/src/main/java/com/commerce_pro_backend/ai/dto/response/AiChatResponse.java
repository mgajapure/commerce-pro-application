package com.commerce_pro_backend.ai.dto.response;

/** Response for a single conversational turn. */
public record AiChatResponse(

    /** Session ID — pass this back on the next turn to continue the conversation. */
    String sessionId,

    /** The model's reply for this turn. */
    String message,

    /** Total turns in the session so far (including this one). */
    int turnCount
) {}
