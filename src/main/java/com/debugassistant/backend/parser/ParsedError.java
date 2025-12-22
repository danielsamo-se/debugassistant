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

}