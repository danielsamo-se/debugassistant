package com.debugassistant.backend.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parses Java stack traces and extracts exception info
 */
@Component
@Slf4j
public class JavaErrorParser implements ErrorParser {

    private final KeywordExtractor keywordExtractor;
    private final RootCauseExtractor rootCauseExtractor;

    public JavaErrorParser(KeywordExtractor keywordExtractor, RootCauseExtractor rootCauseExtractor) {
        this.keywordExtractor = keywordExtractor;
        this.rootCauseExtractor = rootCauseExtractor;
    }

    private static final Pattern EXCEPTION_LINE_PATTERN =
            Pattern.compile("(?:Exception in thread \"[^\"]+\"\\s+)?([a-zA-Z0-9.$_]+(?:Exception|Error))(?::\\s*(.*))?");

    @Override
    public ParsedError parse(String stackTrace) {
        log.debug("Parsing Java stack trace ({} chars)", stackTrace.length());

        List<String> lines = stackTrace.lines().toList();

        String exceptionType = null;
        String message = "";

        // First match as fallback
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            Matcher matcher = EXCEPTION_LINE_PATTERN.matcher(trimmed);
            if (matcher.matches()) {
                exceptionType = extractSimpleName(matcher.group(1));
                message = matcher.group(2) != null ? matcher.group(2).trim() : "";
                log.debug("Found exception: {} - {}", exceptionType, message);
                break;
            }
        }

        if (exceptionType == null) {
            exceptionType = extractFallback(lines);
            log.warn("Used fallback parsing: {}", exceptionType);
        }

        // Root cause overrides fallback
        String rootCauseLine = rootCauseExtractor.extractRootCauseLine(stackTrace);
        String rootCause = rootCauseLine != null ? extractRootCauseType(rootCauseLine) : null;

        if (rootCauseLine != null && !rootCauseLine.isBlank()) {
            Matcher rcMatcher = EXCEPTION_LINE_PATTERN.matcher(rootCauseLine.trim());
            if (rcMatcher.matches()) {
                exceptionType = extractSimpleName(rcMatcher.group(1));
                message = rcMatcher.group(2) != null ? rcMatcher.group(2).trim() : "";
            }
        }

        ParsedError basicError = ParsedError.builder()
                .language("java")
                .exceptionType(exceptionType)
                .message(message)
                .rootCause(rootCause)
                .stackTraceLines(lines.size())
                .build();

        List<String> keywords = keywordExtractor.extract(basicError);

        return ParsedError.builder()
                .language("java")
                .exceptionType(exceptionType)
                .message(message)
                .rootCause(rootCause)
                .keywords(Set.copyOf(keywords))
                .stackTraceLines(lines.size())
                .build();
    }

    // Strip package name
    private String extractSimpleName(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;
    }

    // Split "Type: message"
    private String extractRootCauseType(String rootCauseLine) {
        String[] parts = rootCauseLine.split(":");
        return extractSimpleName(parts[0].trim());
    }

    private String extractFallback(List<String> lines) {
        return lines.stream()
                .map(String::trim)
                .filter(l -> !l.isEmpty())
                .findFirst()
                .orElse("UnknownException");
    }
}