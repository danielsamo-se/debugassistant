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
            "the", "a", "an", "is", "are", "was", "were",
            "be", "been", "being", "have", "has", "had",
            "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "must", "shall",
            "of", "on", "at", "for", "in", "to", "from",
            "with", "by", "about", "into", "through",
            "because", "cannot", "cant", "this", "that",
            "null", "error", "exception", "failed", "line"
    );

    private static final int MAX_QUERY_KEYWORDS = 4;

    public String build(String exceptionType, String message, Set<String> keywords) {
        List<String> parts = new ArrayList<>();

        // exception type is most important
        if (exceptionType != null && !exceptionType.isBlank()) {
            parts.add(exceptionType);
        }

        // add message tokens
        if (message != null && !message.isBlank()) {
            parts.addAll(tokenize(message));
        }

        // add extracted keywords
        if (keywords != null && !keywords.isEmpty()) {
            parts.addAll(cleanKeywords(keywords));
        }

        List<String> cleaned = parts.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> s.length() > 2)
                .filter(s -> !STOP_WORDS.contains(s))
                .distinct()
                .limit(MAX_QUERY_KEYWORDS)
                .toList();

        if (cleaned.isEmpty()) {
            return "exception in:title,body";
        }

        return String.join(" ", cleaned) + " in:title,body";
    }

    private static final int MAX_KEYWORDS = 3;

    public String buildSmartQuery(ParsedError error) {
        List<String> parts = new ArrayList<>();

        if (error.exceptionType() != null && !error.exceptionType().isBlank()) {
            parts.add(error.exceptionType());
        }

        if (error.keywords() != null && !error.keywords().isEmpty()) {
            parts.addAll(
                    cleanKeywords(error.keywords())
                            .stream()
                            .limit(MAX_KEYWORDS)
                            .toList()
            );
        }

        if (parts.isEmpty()) {
            return "exception in:title,body";
        }

        return String.join(" ", parts) + " in:title,body";
    }


    private List<String> tokenize(String text) {
        String[] words = text.split("[\\s:,()\\[\\]{}]+");
        List<String> tokens = new ArrayList<>();
        for (String word : words) {
            String clean = word.replaceAll("[^a-zA-Z0-9]", "").trim();
            if (!clean.isEmpty()) {
                tokens.add(clean);
            }
        }
        return tokens;
    }

    private List<String> cleanKeywords(Set<String> keywords) {
        return keywords.stream()
                .map(String::toLowerCase)
                .map(w -> w.replaceAll("[^a-zA-Z0-9]", ""))
                .filter(w -> w.length() > 2)
                .filter(w -> !STOP_WORDS.contains(w))
                .toList();
    }

    private String cleanWord(String word) {
        return word.replaceAll("[^a-zA-Z]", "").trim();
    }
}