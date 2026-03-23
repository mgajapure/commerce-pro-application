package com.commerce_pro_backend.ai.exception;

/** Thrown when a call is made to an AI feature that has been toggled off in AiConfig. */
public class AiFeatureDisabledException extends AiModuleException {

    public AiFeatureDisabledException(String message) {
        super(message);
    }
}
