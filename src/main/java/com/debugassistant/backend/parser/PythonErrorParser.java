package com.debugassistant.backend.parser;

import com.debugassistant.backend.exception.InvalidStackTraceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parser implementation for Python stack traces.
 */
@Component
@Slf4j
public class PythonErrorParser implements ErrorParser {

    private static final Pattern PYTHON_ERROR_PATTERN =
            Pattern.compile("^([a-zA-Z0-9_.]+):\\s*(.*)$");

    @Override
    public ParsedError parse(String stackTrace) {
        // Defensive check to ensure stability even if called directly
        if (stackTrace == null || stackTrace.isBlank()) {
            throw new InvalidStackTraceException("Stack trace cannot be empty");
        }

        log.debug("Parsing Python stack trace");

        // Split trace into lines and filter out empty ones
        List<String> lines = stackTrace.lines()
                .filter(line -> !line.isBlank())
                .toList();

        if (lines.isEmpty()) {
            throw new InvalidStackTraceException("No readable lines found in stack trace");
        }

        // error is in the last line
        String lastLine = lines.getLast().trim();

        Matcher matcher = PYTHON_ERROR_PATTERN.matcher(lastLine);

        String exceptionType;
        String message;

        if (matcher.matches()) {
            exceptionType = matcher.group(1); // e.g. "ZeroDivisionError"
            message = matcher.group(2);      // e.g. "division by zero"
        } else {
            // Fallback
            log.warn("Could not match standard Python error pattern in line: {}", lastLine);
            exceptionType = "UnknownPythonError";
            message = lastLine;
        }

        log.debug("Parsed Python result: type={}, message={}", exceptionType, message);

        return new ParsedError("python", exceptionType, message);
    }
}
