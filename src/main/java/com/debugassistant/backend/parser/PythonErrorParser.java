package com.debugassistant.backend.parser;

import com.debugassistant.backend.exception.InvalidStackTraceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parses Python stack traces and extracts exception info
 */
@Component
@Slf4j
public class PythonErrorParser implements ErrorParser {

    private final KeywordExtractor keywordExtractor;
    private final RootCauseExtractor rootCauseExtractor;

    public PythonErrorParser(KeywordExtractor keywordExtractor, RootCauseExtractor rootCauseExtractor) {
        this.keywordExtractor = keywordExtractor;
        this.rootCauseExtractor = rootCauseExtractor;
    }

    private static final Pattern PYTHON_ERROR_PATTERN =
            Pattern.compile("^([a-zA-Z0-9_.]+):\\s*(.*)$");

    @Override
    public ParsedError parse(String stackTrace) {
        log.debug("Parsing Python stack trace ({} chars)", stackTrace.length());

        List<String> lines = stackTrace.lines()
                .filter(line -> !line.isBlank())
                .toList();

        if (lines.isEmpty()) {
            throw new InvalidStackTraceException("No readable lines found in stack trace");
        }

        // Python error is in the last line
        String lastLine = lines.getLast().trim();
        Matcher matcher = PYTHON_ERROR_PATTERN.matcher(lastLine);

        String exceptionType;
        String message;

        if (matcher.matches()) {
            exceptionType = matcher.group(1);
            message = matcher.group(2);
        } else {
            log.warn("Could not match Python error pattern: {}", lastLine);
            exceptionType = "UnknownPythonError";
            message = lastLine;
        }

        log.debug("Parsed: exception={}, message={}", exceptionType, message);

        // build basic error first for keyword extraction
        ParsedError basicError = ParsedError.builder()
                .language("python")
                .exceptionType(exceptionType)
                .message(message)
                .stackTraceLines(lines.size())
                .build();

        String rootCause = rootCauseExtractor.extractRootCauseLine(stackTrace);
        List<String> keywords = keywordExtractor.extract(basicError);

        return ParsedError.builder()
                .language("python")
                .exceptionType(exceptionType)
                .message(message)
                .rootCause(rootCause)
                .keywords(Set.copyOf(keywords))
                .stackTraceLines(lines.size())
                .build();
    }
}