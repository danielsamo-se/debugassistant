package com.debugassistant.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeResponse {

    @Schema(description = "Detected programming language", example = "java")
    private String language;

    @Schema(description = "Exception type", example = "java.lang.NullPointerException")
    private String exceptionType;

    @Schema(description = "Exception message", example = "Cannot invoke method on null")
    private String message;

    @Schema(description = "Extracted keywords for search")
    private Set<String> keywords;

    @Schema(description = "Root cause exception if exists", example = "java.sql.SQLException")
    private String rootCause;

    @Schema(description = "Dummy score (will be replaced in Phase 2)", example = "42")
    private int score;

    @Schema(description = "GitHub search results (empty in Phase 1)")
    private List<SearchResult> results;

    @Schema(description = "Analysis timestamp")
    private Instant timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult {
        private String source;
        private String title;
        private String url;
        private Integer reactions;
        private Instant createdAt;
        private Double score;
    }
}