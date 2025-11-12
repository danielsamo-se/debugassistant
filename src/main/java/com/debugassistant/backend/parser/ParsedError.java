package com.debugassistant.backend.parser;

import lombok.Builder;
import java.util.Set;

/**
 * Holds the extracted info from a stack trace
 */
@Builder
public record ParsedError(
        String language,
        String exceptionType,
        String message,
        Set<String> keywords,
        String rootCause,
        Integer stackTraceLines
) {
    // constructor for basic cases
    public ParsedError(String language, String exceptionType, String message) {
        this(language, exceptionType, message, Set.of(), null, null);
    }

    public ParsedError(String language, String exceptionType, String message,
                       String rootCause, Integer stackTraceLines) {
        this(language, exceptionType, message, Set.of(), rootCause, stackTraceLines);
    }
}