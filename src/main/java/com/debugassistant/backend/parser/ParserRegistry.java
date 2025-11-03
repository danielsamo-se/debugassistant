package com.debugassistant.backend.parser;

import com.debugassistant.backend.exception.InvalidStackTraceException;
import com.debugassistant.backend.exception.UnsupportedLanguageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Decides which parser to use based on the stack trace content.
 * Currently supports Java and Python.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ParserRegistry {

    private final JavaErrorParser javaErrorParser;
    private final PythonErrorParser pythonErrorParser;

    public ParsedError parse(String stackTrace) {
        if (stackTrace == null || stackTrace.isBlank()) {
            throw new InvalidStackTraceException("Stack trace cannot be empty");
        }

        String detectedLanguage = detectLanguage(stackTrace);
        log.debug("Detected language: {}", detectedLanguage);

        return switch (detectedLanguage) {
            case "java" -> javaErrorParser.parse(stackTrace);
            case "python" -> pythonErrorParser.parse(stackTrace);
            default -> throw new UnsupportedLanguageException(
                    "Language '" + detectedLanguage + "' is not supported yet."
            );
        };
    }

    private String detectLanguage(String stackTrace) {
        // typical stack trace elements like "com.example.class" or ".java:LineNumber"
        if (stackTrace.contains("at ") &&
                (stackTrace.contains(".java:") || stackTrace.contains("Exception"))) {
            return "java";
        }
        // usually starts with "Traceback" or patterns
        if (stackTrace.contains("Traceback") ||
                (stackTrace.contains("File \"") && stackTrace.contains(", line "))) {
            return "python";
        }

        return "unknown";
    }
}