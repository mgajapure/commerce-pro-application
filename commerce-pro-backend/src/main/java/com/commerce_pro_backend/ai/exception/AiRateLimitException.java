package com.commerce_pro_backend.ai.exception;

/** Thrown when a per-feature per-minute or per-hour rate limit is exceeded. */
public class AiRateLimitException extends AiModuleException {

    public AiRateLimitException(String message) {
        super(message);
    }
}
