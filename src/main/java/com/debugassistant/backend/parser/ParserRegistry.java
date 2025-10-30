package com.debugassistant.backend.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParserRegistry {

    private final JavaErrorParser javaErrorParser;

    // until now only java
    public ParsedError parse(String stackTrace) {

        if (stackTrace == null || stackTrace.isBlank()) {
            return new ParsedError("unknown", "unknown", "empty stacktrace");
        }

        // simple detection for now
        if (isJavaStackTrace(stackTrace)) {
            return javaErrorParser.parse(stackTrace);
        }

        // fallback
        return new ParsedError("unknown", "unknown", "unrecognized format");
    }

    // first naive detection
    private boolean isJavaStackTrace(String stackTrace) {
        return stackTrace.contains("Exception")
                || stackTrace.contains("java.")
                || stackTrace.startsWith("java");
    }
}