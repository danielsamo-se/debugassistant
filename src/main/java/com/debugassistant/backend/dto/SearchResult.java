package com.debugassistant.backend.dto;

/**
 * A single search result from GitHub or Stack Overflow
 */
public record SearchResult(
        String source,
        String title,
        String url,
        Integer reactions,
        String snippet,
        Double score,
        Integer answerCount,
        Boolean isAnswered
) {
    public Double getScore() {
        return score != null ? score : 0.0;
    }

}