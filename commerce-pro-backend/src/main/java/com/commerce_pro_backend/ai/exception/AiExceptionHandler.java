package com.commerce_pro_backend.ai.exception;

import com.commerce_pro_backend.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Handles all AI module exceptions before the global handler picks them up.
 * Returns the same {@link ApiResponse} envelope used throughout the application.
 */
@Slf4j
@RestControllerAdvice
@Order(1)
public class AiExceptionHandler {

    @ExceptionHandler(AiFeatureDisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeatureDisabled(
            AiFeatureDisabledException ex, HttpServletRequest request) {
        log.warn("AI feature disabled: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse("AI_DISABLED", ex.getMessage(), request));
    }

    @ExceptionHandler(AiBudgetExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleBudgetExceeded(
            AiBudgetExceededException ex, HttpServletRequest request) {
        log.warn("AI budget exceeded: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(errorResponse("AI_BUDGET_EXCEEDED", ex.getMessage(), request));
    }

    @ExceptionHandler(AiRateLimitException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimit(
            AiRateLimitException ex, HttpServletRequest request) {
        log.warn("AI rate limit hit: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(errorResponse("AI_RATE_LIMITED", ex.getMessage(), request));
    }

    @ExceptionHandler(AiResponseParseException.class)
    public ResponseEntity<ApiResponse<Void>> handleParseFailed(
            AiResponseParseException ex, HttpServletRequest request) {
        log.error("AI response parse error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse("AI_PARSE_ERROR", ex.getMessage(), request));
    }

    @ExceptionHandler(ConversationExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleConversationExpired(
            ConversationExpiredException ex, HttpServletRequest request) {
        log.info("AI conversation expired: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.GONE)
                .body(errorResponse("SESSION_EXPIRED", ex.getMessage(), request));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ApiResponse<Void> errorResponse(String code, String message, HttpServletRequest request) {
        return ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .error(Map.of("code", code))
                .timestamp(System.currentTimeMillis())
                .path(request.getRequestURI())
                .build();
    }
}
