package com.commerce_pro_backend.ai.exception;

/** Base exception for all AI module errors. */
public class AiModuleException extends RuntimeException {

    public AiModuleException(String message) {
        super(message);
    }

    public AiModuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
