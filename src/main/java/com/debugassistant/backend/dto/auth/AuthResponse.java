package com.debugassistant.backend.dto.auth;

/**
 * Response DTO for authentication: login and register
 */
public record AuthResponse(
        String token,
        long expiresIn,
        String email,
        String name
) {}