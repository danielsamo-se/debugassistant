package com.debugassistant.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for authentication: login and register
 */
public record AuthResponse(
        @Schema(description = "JWT bearer token")
        String token,

        @Schema(description = "Token validity duration in milliseconds", example = "86400000")
        long expiresIn,

        @Schema(description = "User email", example = "user@example.com")
        String email,

        @Schema(description = "User display name", example = "Max Mustermann", nullable = true)
        String name
) {}