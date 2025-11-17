package com.debugassistant.backend.parser;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Extracts a small set of relevant keywords to improve GitHub search queries
 */
@Component
public class KeywordExtractor {

    // noise
    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "or", "to", "of", "a", "is", "in",
            "for",  "on", "at", "by", "from", "with", "this",
            "that",  "exception", "error", "failed", "cause",
            "stack",  "trace", "null", "line", "java", "python"
    );

    private static final int MAX_KEYWORDS = 5;

    public List<String> extract(ParsedError error) {
        if (error == null) {
            return List.of();
        }

        Map<String, Double> scores = new HashMap<>();

        // exception info is usually most relevant
        addTokens(scores, error.exceptionType(), 3.0);

        // root cause often contains specific hints
        addTokens(scores, error.rootCause(), 2.0);

        // message may add extra context
        addTokens(scores, error.message(), 1.0);

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(MAX_KEYWORDS)
                .map(Map.Entry::getKey)
                .toList();
    }

    private void addTokens(Map<String, Double> scores, String text, double baseScore) {
        if (text == null || text.isBlank()) {
            return;
        }

        String[] parts = text.split("[\\s:/,()\\[\\]{}]+");

        for (String raw : parts) {
            String word = raw.toLowerCase().trim();
            if (word.length() < 3) continue;
            if (STOPWORDS.contains(word)) continue;
            if (isDynamicStopword(word)) continue;

            double score = baseScore;

            if (word.length() >= 6) {
                score += 0.5;
            }

            scores.merge(word, score, Double::sum);
        }
    }

    private boolean isDynamicStopword(String word) {
        return word.matches("[0-9]+") ||
                word.matches("[0-9a-f]{6,}") ||
                word.contains("/") ||
                word.contains("\\") ||
                word.endsWith(".java") ||
                word.endsWith(".py");
    }
}