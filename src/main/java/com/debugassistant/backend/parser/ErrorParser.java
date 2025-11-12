package com.debugassistant.backend.parser;

/**
 * Interface for language specific stacktrace parsers
 */
public interface ErrorParser {
    ParsedError parse(String stackTrace);
}