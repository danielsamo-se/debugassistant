package com.debugassistant.backend.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Standard error response for API errors
 */
public record ErrorResponse(
        @Schema(description = "Human readable error message", example = "Invalid JSON payload")
        String message,

        @Schema(description = "Timestamp when the error occurred", example = "2025-12-29T12:34:56Z")
        Instant timestamp,

        @Schema(description = "Request path (if available)", example = "/api/analyze")
        String path
) {
    public ErrorResponse(String message, Instant timestamp) {
        this(message, timestamp, null);
    }
}