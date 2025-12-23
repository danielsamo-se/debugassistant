package com.debugassistant.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.Set;

/**
 * Response containing parsed error info and search results
 */
@Builder
public record AnalyzeResponse(

        @Schema(description = "Detected programming language", example = "java")
        String language,

        @Schema(description = "Exception type", example = "NullPointerException")
        String exceptionType,

        @Schema(description = "Exception message", example = "Cannot invoke method on null")
        String message,

        @Schema(description = "Extracted keywords used for search")
        Set<String> keywords,

        @Schema(description = "Root cause if found", example = "SQLException")
        String rootCause,

        @Schema(description = "Search results from GitHub and Stack Overflow")
        List<SearchResult> results

) {}