package com.debugassistant.backend.parser;

import com.debugassistant.backend.exception.InvalidStackTraceException;
import com.debugassistant.backend.exception.UnsupportedLanguageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Picks the right parser based on stack trace using a scoring system
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

        String normalized = stackTrace.toLowerCase(Locale.ROOT);

        int javaScore = scoreJava(normalized);
        int pythonScore = scorePython(normalized);

        log.debug("Language scores - Java: {}, Python: {}", javaScore, pythonScore);

        if (javaScore == 0 && pythonScore == 0) {
            throw new UnsupportedLanguageException("Could not detect language from stack trace");
        }

        // Java wins ties
        if (javaScore >= pythonScore) {
            log.debug("Selected parser: Java");
            return javaErrorParser.parse(stackTrace);
        } else {
            log.debug("Selected parser: Python");
            return pythonErrorParser.parse(stackTrace);
        }
    }

    private int scoreJava(String s) {
        int score = 0;

        // Strong indicators
        if (s.contains("exception in thread")) score += 3;
        if (s.contains("java.lang.")) score += 3;

        // Common exceptions
        if (s.contains("nullpointerexception")) score += 2;
        if (s.contains("classnotfoundexception")) score += 2;

        // Stack frame patterns
        if (s.contains(".java:")) score += 2;
        if (s.contains("at ") && s.contains("(")) score += 1;

        // Modern Java
        if (s.contains("java.base/")) score += 1;
        if (s.contains("virtualthread")) score += 1;

        return score;
    }

    private int scorePython(String s) {
        int score = 0;

        // Strong indicators
        if (s.contains("traceback")) score += 3;

        // Stack frame patterns
        if (s.contains("file \"") && s.contains(", line ")) score += 2;

        // Common exceptions
        if (s.contains("valueerror")) score += 2;
        if (s.contains("keyerror")) score += 2;
        if (s.contains("typeerror")) score += 2;

        if (s.contains("most recent call last")) score += 1;

        return score;
    }
}