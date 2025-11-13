package com.debugassistant.backend.dto;

/**
 * A single search result from GitHub
 */
public record SearchResult(
        String source,
        String title,
        String url,
        Integer reactions,
        String snippet,
        Double score
) {
    // for sorting compatibility
    public Double getScore() {
        return score != null ? score : 0.0;
    }
}