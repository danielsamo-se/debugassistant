package com.debugassistant.backend.parser;

import com.debugassistant.backend.exceptions.InvalidStackTraceException;
import com.debugassistant.backend.exceptions.UnsupportedLanguageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ParserRegistry {

    private final JavaErrorParser javaErrorParser;

    public ParsedError parse(String stackTrace) {
        if (stackTrace == null || stackTrace.isBlank()) {
            throw new InvalidStackTraceException("Stack trace cannot be empty");
        }

        //language detection
        String detectedLanguage = detectLanguage(stackTrace);

        log.debug("Detected language: {}", detectedLanguage);

        return switch (detectedLanguage) {
            case "java" -> javaErrorParser.parse(stackTrace);
            default -> throw new UnsupportedLanguageException(
                    "Language '" + detectedLanguage + "' is not supported yet. " +
                            "Currently supported: Java"
            );
        };
    }

    private String detectLanguage(String stackTrace) {
        // Java detection
        if (stackTrace.contains("at ") &&
                (stackTrace.contains(".java:") || stackTrace.contains("Exception"))) {
            return "java";
        }

        // Python detection
        if (stackTrace.contains("Traceback") && stackTrace.contains("File \"")) {
            return "python";
        }

        return "unknown";
    }
}