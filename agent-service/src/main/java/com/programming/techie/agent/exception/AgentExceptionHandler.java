package com.programming.techie.agent.exception;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Global exception handler for agent-service.
 * Returns structured JSON error responses for all failure modes.
 */
@Slf4j
@RestControllerAdvice
public class AgentExceptionHandler {

    // ── Rate limiter exceeded (spam protection) ──
    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RequestNotPermitted ex) {
        log.warn("[AgentExceptionHandler] Rate limit exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                        "error", "Rate limit exceeded",
                        "message", "Too many requests. Please wait before sending another message.",
                        "timestamp", Instant.now().toString()
                ));
    }

    // ── Authorization denied (tool authorization) ──
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(Exception ex) {
        log.warn("[AgentExceptionHandler] Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "error", "Forbidden",
                        "message", "You do not have permission to perform this action.",
                        "timestamp", Instant.now().toString()
                ));
    }

    // ── Catch-all ──
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("[AgentExceptionHandler] Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Internal server error",
                        "message", "An unexpected error occurred. Please try again.",
                        "timestamp", Instant.now().toString()
                ));
    }
}
