package com.commerce_pro_backend.ai.exception;

/** Thrown when a daily or monthly spend cap (global or per-feature) is exhausted. */
public class AiBudgetExceededException extends AiModuleException {

    public AiBudgetExceededException(String message) {
        super(message);
    }
}
