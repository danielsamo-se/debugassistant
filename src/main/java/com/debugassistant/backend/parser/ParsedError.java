package com.debugassistant.backend.parser;

public record ParsedError(
        String language,
        String exceptionType,
        String message
) {}