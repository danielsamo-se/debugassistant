package com.debugassistant.backend.parser;

import org.springframework.stereotype.Component;

@Component
public class JavaErrorParser implements ErrorParser {

    @Override
    public ParsedError parse(String stackTrace) {

        if (stackTrace == null || stackTrace.isBlank()) {
            return new ParsedError("unknown", "unknown", "empty stacktrace");
        }

        // first line contains exception and message
        String firstLine = stackTrace.lines().findFirst().orElse("").trim();


        String language = "java";
        String exceptionType = extractException(firstLine);
        String message = extractMessage(firstLine);

        return new ParsedError(language, exceptionType, message);
    }

    // extract exception before first colon
    private String extractException(String firstLine) {
        if (!firstLine.contains(":")) {
            return firstLine; // fallback
        }

        return firstLine.substring(0, firstLine.indexOf(":")).trim();
    }

    // extract message after first colon
    private String extractMessage(String firstLine) {
        if (!firstLine.contains(":")) {
            return "";
        }

        return firstLine.substring(firstLine.indexOf(":") + 1).trim();
    }
}