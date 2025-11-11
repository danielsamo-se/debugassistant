package com.debugassistant.backend.dto;

public record SearchResult(
        String source,
        String title,
        String url,
        Integer reactions,
        String snippet,
        Double score
) {}