package com.debugassistant.backend.parser;

public interface ErrorParser {
    ParsedError parse(String stackTrace);
}