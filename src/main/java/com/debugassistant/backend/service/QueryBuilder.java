package com.debugassistant.backend.service;

import com.debugassistant.backend.parser.ParsedError;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class QueryBuilder {

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
        // Entfernt Sonderzeichen, Zahlen, Quotes etc.
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
