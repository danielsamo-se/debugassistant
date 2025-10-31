package com.debugassistant.backend.parser;

import lombok.Builder;

import java.time.Instant;
import java.util.Set;

@Builder
public record ParsedError(
        String language,
        String exceptionType,
        String message,
        Set<String> keywords,
        String rootCause,
        Integer stackTraceLines,
        Instant parsedAt
) {
    public ParsedError(String language, String exceptionType, String message) {
        this(language, exceptionType, message,
                Set.of(),
                null,
                null,
                Instant.now());
    }
}