package com.debugassistant.backend.parser;

import com.debugassistant.backend.exception.InvalidStackTraceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class JavaErrorParser implements ErrorParser {

    // ExceptionType: Message
    private static final Pattern EXCEPTION_PATTERN =
            Pattern.compile("^([a-zA-Z0-9.$_]+)(?::\\s*(.*))?$");

    @Override
    public ParsedError parse(String stackTrace) {
        if (stackTrace == null || stackTrace.isBlank()) {
            throw new InvalidStackTraceException("Stack trace cannot be empty");
        }

        log.debug("Parsing Java stack trace");

        // first line
        String firstLine = stackTrace.lines()
                .findFirst()
                .orElseThrow(() -> new InvalidStackTraceException("Empty stack trace"));
        // extraction with regex
        Matcher matcher = EXCEPTION_PATTERN.matcher(firstLine.trim());

        String exceptionType;
        String message;

        if (matcher.matches()) {
            exceptionType = matcher.group(1);
            message = matcher.group(2) != null ? matcher.group(2).trim() : "";
        } else {
            exceptionType = firstLine.trim();
            message = "";
        }

        log.debug("Parsed: exception={}, message={}", exceptionType, message);

        return new ParsedError("java", exceptionType, message);
    }
}