package com.debugassistant.backend.service;

import com.debugassistant.backend.parser.ParsedError;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Builds search queries for GitHub Issue Search
 */
@Component
public class QueryBuilder {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "must", "shall", "of", "on", "at", "for",
            "in", "to", "from", "with", "by", "about", "into", "through",
            "because", "cannot", "cant", "this", "that",
            "null", "error", "exception", "failed", "line",
            "caused", "java", "lang", "class", "org", "com", "net", "io"
    );

    private static final Set<String> UMBRELLA_ORGS = Set.of(
            "apache", "google", "amazon", "aws", "azure", "eclipse",
            "jakarta", "github", "software"
    );

    private static final Set<String> GENERIC_PACKAGE_PARTS = Set.of(
            "internal", "util", "utils", "common", "core", "impl",
            "exception", "error", "api", "spi"
    );

    private static final int MAX_KEYWORDS = 3;

    public String buildSmartQuery(ParsedError error, String rawStackTrace) {
        List<String> parts = new ArrayList<>();

        String rawExceptionString = error.exceptionType();
        if (error.rootCause() != null && !error.rootCause().isBlank()) {
            rawExceptionString = error.rootCause().split(":")[0];
        }

        String library = extractLibraryName(rawExceptionString);
        if (library != null) {
            parts.add(library);
        }

        String simpleException = getSimpleName(rawExceptionString);
        if (simpleException != null && !simpleException.isBlank()) {
            parts.add(simpleException);
        }

        if (error.keywords() != null && !error.keywords().isEmpty()) {
            parts.addAll(
                    cleanKeywords(error.keywords())
                            .stream()
                            .limit(MAX_KEYWORDS)
                            .toList()
            );
        }

        // remove duplicates
        List<String> finalParts = parts.stream().distinct().toList();

        // fallback
        if (finalParts.isEmpty()) {
            return "exception in:title,body";
        }

        return String.join(" ", finalParts) + " in:title,body";
    }

    private String extractLibraryName(String fullClassName) {
        // requires package structure
        if (fullClassName == null || !fullClassName.contains(".")) return null;

        String[] parts = fullClassName.split("\\.");

        // not enough segments
        if (parts.length < 3) return null;

        //typical position
        int targetIndex = 1;
        String candidate = parts[targetIndex].toLowerCase();

        // skip umbrella logs
        if (UMBRELLA_ORGS.contains(candidate) && parts.length > 3) {
            targetIndex++;
            candidate = parts[targetIndex].toLowerCase();
        }

        // skip generic names
        if (candidate.length() <= 2 || GENERIC_PACKAGE_PARTS.contains(candidate)) {
            return null;
        }

        return candidate;
    }

    private List<String> cleanKeywords(Set<String> keywords) {
        return keywords.stream()
                .map(String::toLowerCase)
                .map(w -> w.replaceAll("[^a-zA-Z0-9]", ""))
                .filter(w -> w.length() > 2)
                .filter(w -> !STOP_WORDS.contains(w))
                .toList();
    }

    private String getSimpleName(String fullClassName) {
        if (fullClassName == null) return "";
        if (fullClassName.contains(".")) {
            return fullClassName.substring(fullClassName.lastIndexOf('.') + 1).trim();
        }
        return fullClassName.trim();
    }
}