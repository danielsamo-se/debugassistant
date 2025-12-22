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

    // Generic filler words
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

    // unnecessary packgae names
    private static final Set<String> GENERIC_PACKAGE_PARTS = Set.of(
            "internal", "util", "utils", "common", "core", "impl",
            "exception", "error", "api", "spi"
    );

    private static final int MAX_KEYWORDS = 3;

    public String buildSmartQuery(ParsedError error, String rawStackTrace) {
        List<String> parts = new ArrayList<>();

        // Prefer root cause over top exception
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

    public List<String> buildStackOverflowQueries(ParsedError parsed, String rawStackTrace) {
        String msg = sanitizeMessage(parsed.message());
        String phrase = extractStrongPhrase(msg);
        String qA = joinNonEmpty(msg, phraseQuoted(phrase));
        String qB = phraseQuoted(phrase);

        return List.of(qA, qB);
    }

    public String buildGitHubQuery(ParsedError error, String rawStackTrace) {
        return buildSmartQuery(error, rawStackTrace);
    }

    // Remove search operators, paths, hex, long quotes
    private String sanitizeMessage(String message) {
        if (message == null) return "";
        String s = message.trim();

        s = s.replace("in:title,body", " ");
        s = s.replaceAll("\\s+", " ").trim();

        // remove paths, very rough
        s = s.replaceAll("[A-Za-z]:\\\\[^\\s]+", " ");
        s = s.replaceAll("/[^\\s]+", " ");

        // remove hex addresses
        s = s.replaceAll("0x[0-9a-fA-F]+", "0xX");

        // shrink quotes noise a bit
        s = s.replaceAll("\"[^\"]{60,}\"", "\"...\"");

        return s.replaceAll("\\s+", " ").trim();
    }

    // Pick strong phrase if possible
    private String extractStrongPhrase(String msg) {
        if (msg == null) return "";

        String s = msg;

        // common Java NPE phrasing
        if (s.contains("Cannot invoke")) return "Cannot invoke";
        if (s.contains("NullPointerException")) return "NullPointerException";

        String m = s.replaceAll(".*?([A-Za-z0-9_]+\\.[A-Za-z0-9_]+).*", "$1");
        if (!m.equals(s) && m.contains(".")) return m;

        return "";
    }

    // Quote phrases for better matches
    private String phraseQuoted(String phrase) {
        if (phrase == null) return "";
        String p = phrase.trim();
        if (p.isBlank()) return "";

        // Quote phrases and dotted tokens to boost precision
        if (p.contains(" ") || p.contains(".")) {
            return "\"" + p.replace("\"", "") + "\"";
        }
        return p;
    }

    // Join without duplicates
    private String joinNonEmpty(String a, String b) {
        String x = (a == null ? "" : a.trim());
        String y = (b == null ? "" : b.trim());

        if (x.isBlank() && y.isBlank()) return "";
        if (x.isBlank()) return y;
        if (y.isBlank()) return x;

        // avoid duplicate if b already contained
        if (x.contains(y)) return x;

        return (x + " " + y).trim();
    }
}