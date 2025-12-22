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
            "user", "password", "username", "account",
            "available", "qualifying",
            "because", "cannot", "invoke",
            "authentication", "failed"
    );

    private static final int MAX_KEYWORDS = 5;

    public List<String> extract(ParsedError error) {
        if (error == null) {
            return List.of();
        }

        Map<String, Double> scores = new HashMap<>();

        // Simple weights: type > root cause > message
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

        // Split into tokens
        String[] parts = text.split("[\\s:/,()\\[\\]{}]+");

        for (String raw : parts) {
            String word = raw.toLowerCase().trim();

            // Keep only safe chars for search
            word = word.replace("\"", "");
            word = word.replaceAll("[^a-z0-9_.]+", "");

            if (word.length() < 3) continue;
            if (STOPWORDS.contains(word)) continue;
            if (isDynamicStopword(word)) continue;

            double score = baseScore;

            // Prefer longer words
            if (word.length() >= 6) {
                score += 0.5;
            }

            scores.merge(word, score, Double::sum);
        }
    }

    // Filter IDs, paths, hashes, file names
    private boolean isDynamicStopword(String word) {
        return word.matches("[0-9]+") ||
                word.matches("[0-9a-f]{6,}") ||
                word.contains("/") ||
                word.contains("\\") ||
                word.endsWith(".java") ||
                word.endsWith(".py") ||
                // neu:
                word.matches("'.+'") ||
                word.matches("\".+\"") ||
                word.matches("[a-z0-9_]{3,}") && word.equals(word.toLowerCase()) && word.length() <= 20 && word.contains("_"); // typische user_ids
    }


}