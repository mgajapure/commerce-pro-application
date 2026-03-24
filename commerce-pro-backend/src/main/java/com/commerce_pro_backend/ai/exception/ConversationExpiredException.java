package com.commerce_pro_backend.ai.exception;

/** Thrown when a client attempts to continue a multi-turn session that has expired. */
public class ConversationExpiredException extends AiModuleException {

    public ConversationExpiredException(String message) {
        super(message);
    }
}
