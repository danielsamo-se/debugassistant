package com.debugassistant.backend.exception;

import java.time.Instant;

/**
 * Standard error response for API errors
 */
public record ErrorResponse(
        String message,
        Instant timestamp
) {}