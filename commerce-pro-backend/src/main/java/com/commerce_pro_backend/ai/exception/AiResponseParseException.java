package com.commerce_pro_backend.ai.exception;

/** Thrown when the model returns a response that cannot be parsed into the expected structure. */
public class AiResponseParseException extends AiModuleException {

    public AiResponseParseException(String message) {
        super(message);
    }

    public AiResponseParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
