package com.debugassistant.backend.service;

import com.debugassistant.backend.parser.ParsedError;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Builds search queries for GitHub Issue Search
 * Combines exception, message tokens and extracted keywords
 */

@Component
public class QueryBuilder {


    /**
     * Builds a simple query from a ParsedError
     */
    public String buildSmartQuery(ParsedError error) {

        String base = error.exceptionType() != null
                ? error.exceptionType().trim()
                : "";

        List<String> cleanedKeywords = cleanKeywords(error.keywords());
        String keywordPart = String.join(" ", cleanedKeywords);

        String core = base;
        if (!keywordPart.isBlank()) {
            core = (core + " " + keywordPart).trim();
        }

        return (core + " in:title,body").trim();
    }

    /**
     * Creates a normalized search query and produces tokens to improve the search
     */
    public String build(String exceptionType, String message, Set<String> keywords) {

        List<String> parts = new ArrayList<>();

        if (exceptionType != null && !exceptionType.isBlank()) {
            parts.add(exceptionType.toLowerCase());
        }

        if (message != null && !message.isBlank()) {
            parts.addAll(cleanKeywords(Set.of(message.split(" "))));
        }

        if (keywords != null && !keywords.isEmpty()) {
            parts.addAll(cleanKeywords(keywords));
        }

        List<String> cleaned = parts.stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();

        String core = String.join(" ", cleaned);

        return (core + " in:title,body").trim();
    }

    /**
     * Normalize and filter keywords
     */
    private List<String> cleanKeywords(Set<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of();
        }

        return keywords.stream()
                .map(String::toLowerCase)
                .map(this::cleanWord)
                .filter(w -> w.length() > 2)
                .filter(w -> !STOP_WORDS.contains(w))
                .sorted()
                .limit(3)
                .toList();
    }

    private String cleanWord(String word) {
        return word
                .replaceAll("[^a-zA-Z]", "")
                .trim();
    }

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "because", "cannot", "cant",
            "line", "null", "error", "exception",
            "of", "on", "at", "for", "in", "to", "from"
    );

}
