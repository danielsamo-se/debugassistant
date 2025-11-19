package com.debugassistant.backend.dto.history;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for saving a search history entry
 */
public record SaveHistoryRequest(
        @NotBlank String stackTraceSnippet,
        String language,
        String exceptionType,
        String searchUrl
) {}