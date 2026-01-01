package com.debugassistant.backend.service;

import com.debugassistant.backend.parser.ParsedError;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        String exception = error.exceptionType();

        String root = (error.rootCause() != null && !error.rootCause().isBlank())
                ? error.rootCause().split(":")[0]
                : null; // class token only

        String library = extractLibraryName(exception);
        if (library != null) {
            parts.add(library);
        }

        String simpleException = getSimpleName(exception);
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
        List<String> finalParts = parts.stream().distinct().toList(); // dedup tokens

        // fallback
        if (finalParts.isEmpty()) {
            return "exception in:title,body"; // fallback
        }

        return String.join(" ", finalParts) + " in:title,body"; // scope to fields
    }

    private String extractLibraryName(String fullClassName) {
        // non-package class name
        if (fullClassName == null || !fullClassName.contains(".")) return null;

        // ignore JDK namespaces
        if (fullClassName.startsWith("java.") || fullClassName.startsWith("javax.") || fullClassName.startsWith("jakarta.")) {
            return null;
        }

        String[] parts = fullClassName.split("\\.");

        // not enough segments
        if (parts.length < 3) return null;

        // typical org.group.* pattern
        int targetIndex = 1;
        String candidate = parts[targetIndex].toLowerCase();

        // skip umbrella logs
        if (UMBRELLA_ORGS.contains(candidate) && parts.length > 3) {
            targetIndex++;
            candidate = parts[targetIndex].toLowerCase(); // shift to real lib
        }

        if (candidate.length() <= 2 || GENERIC_PACKAGE_PARTS.contains(candidate)) {
            return null; // low-value token
        }

        return candidate;
    }

    private List<String> cleanKeywords(Set<String> keywords) {
        return keywords.stream()
                .map(String::toLowerCase)
                .map(w -> w.replaceAll("[^a-zA-Z0-9]", ""))
                .filter(w -> w.length() > 2) // drop short tokens
                .filter(w -> !STOP_WORDS.contains(w)) // stop-word filter
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
        List<String> queries = new ArrayList<>();

        String exception = getSimpleName(parsed.exceptionType()); // main anchor
        if (exception == null || exception.isBlank()) {
            exception = "Exception"; // fallback anchor
        }

        String messageStart = extractFirstWords(parsed.message(), 4);  // add specificity

        if (!messageStart.isBlank()) {
            queries.add(exception + " " + messageStart);
        }

        queries.add(exception); // broad fallback

        return queries.stream()
                .filter(q -> q != null && !q.isBlank())
                .distinct()
                .toList();
    }

    private String extractFirstWords(String message, int count) {
        if (message == null || message.isBlank()) return "";

        String[] words = message.split("\\s+");
        return Arrays.stream(words)
                .limit(count)
                .collect(Collectors.joining(" "));
    }

    public List<String> buildGitHubQueries(ParsedError error, String rawStackTrace) {
        String simpleException = extractSimpleException(error);
        String context = detectGitHubContextToken(error.exceptionType(), rawStackTrace.toLowerCase());
        boolean looksSpring = detectSpringError(simpleException, rawStackTrace, error.message());
        List<String> keywords = extractUsefulKeywords(error);
        boolean generic = isGenericExceptionName(simpleException);

        List<String> queries = new ArrayList<>();

        // onion queries: high precision -> broad fallback
        addSpringQueries(queries, simpleException, looksSpring); // DI/Bean patterns
        addContextQueries(queries, simpleException, context); // framework context
        addKeywordQueries(queries, simpleException, context, keywords); // precision boost
        addGenericQueries(queries, simpleException, context, keywords, generic); // fallback layer

        return deduplicateQueries(queries);
    }

    private String extractSimpleException(ParsedError error) {
        String exception = error.exceptionType();

        // Prefer root cause
        if (error.rootCause() != null && !error.rootCause().isBlank()) {
            exception = error.rootCause().split(":")[0];
        }

        String simpleException = getSimpleName(exception);
        if (simpleException == null || simpleException.isBlank()) {
            simpleException = "Exception";
        }

        return simpleException;
    }

    private boolean detectSpringError(String simpleException, String rawStackTrace, String message) {
        String stackLower = rawStackTrace == null ? "" : rawStackTrace.toLowerCase();
        String msgLower = message == null ? "" : message.toLowerCase();
        String context = detectGitHubContextToken(simpleException, stackLower); // reuse context

        return stackLower.contains("org.springframework")
                || (context != null && context.equals("spring"))
                || simpleException.toLowerCase().contains("bean")
                || msgLower.contains("no qualifying bean")
                || msgLower.contains("nosuchbeandefinition"); // Spring DI signatures
    }

    private List<String> extractUsefulKeywords(ParsedError error) {
        if (error.keywords() == null) return List.of();

        return cleanKeywords(error.keywords())
                .stream()
                .filter(this::isUsefulGitHubKeyword) // drop noisy tokens
                .limit(3) // query length cap
                .toList();
    }

    private void addSpringQueries(List<String> queries, String simpleException, boolean looksSpring) {
        if (looksSpring) {
            queries.add(simpleException + " spring in:title,body");
            queries.add("\"no qualifying bean\" spring in:title,body"); // exact phrase
            queries.add(simpleException + " in:title spring");
        }
    }

    private void addContextQueries(List<String> queries, String simpleException, String context) {
        if (context != null && !context.isBlank()) {
            queries.add(simpleException + " " + context + " in:title,body");
            queries.add(simpleException + " in:title " + context);
        }
    }

    private void addKeywordQueries(List<String> queries, String simpleException, String context, List<String> keywords) {
        if (!keywords.isEmpty()) {
            List<String> k2 = keywords.size() > 2 ? keywords.subList(0, 2) : keywords; // token cap
            queries.add(simpleException + " " + String.join(" ", k2) + " in:title,body");

            if (context != null && !context.isBlank()) {
                queries.add(simpleException + " " + context + " " + String.join(" ", k2) + " in:title,body");
            }
        }
    }

    private void addGenericQueries(List<String> queries, String simpleException, String context, List<String> keywords, boolean generic) {
        if (!generic) {
            queries.add(simpleException + " in:title,body"); // broad fallback
            queries.add(simpleException + " in:title");  // title-only precision
        } else {
            if (context != null && !context.isBlank()) {
                queries.add(simpleException + " " + context + " in:title,body");
            }
            if (!keywords.isEmpty()) {
                queries.add(simpleException + " " + keywords.getFirst() + " in:title,body");
            }
        }
    }

    private List<String> deduplicateQueries(List<String> queries) {
        return queries.stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }

    private String detectGitHubContextToken(String exception, String stackLower) {
        String library = extractLibraryName(exception); // package hint
        String lib = normalizeLibraryToken(library);
        if (lib != null) return lib; // early context

        String s = stackLower == null ? "" : stackLower;

        if (s.contains("org.springframework")) return "spring";
        if (s.contains("org.hibernate")) return "hibernate";
        if (s.contains("jakarta.persistence") || s.contains("javax.persistence")) return "jpa";  // namespace hint
        if (s.contains("com.fasterxml.jackson")) return "jackson";
        if (s.contains("org.junit") || s.contains("org.mockito")) return "test";

        return null;
    }

    private String normalizeLibraryToken(String library) {
        if (library == null || library.isBlank()) return null;
        String l = library.toLowerCase();

        if (l.contains("spring")) return "spring";
        if (l.contains("hibernate")) return "hibernate";
        if (l.contains("jakarta") || l.contains("persistence") || l.contains("jpa")) return "jpa"; // normalize variants
        if (l.contains("jackson") || l.contains("fasterxml")) return "jackson";

        return library.trim();
    }

    private boolean isGenericExceptionName(String simpleException) {
        if (simpleException == null) return true;
        String e = simpleException.toLowerCase();

        return Set.of(
                "exception",
                "runtimeexception",
                "error",
                "throwable",
                "executionexception",
                "completionexception",
                "ioexception",
                "sqlexception",
                "nullpointerexception"
        ).contains(e); // generic names
    }

    private boolean isUsefulGitHubKeyword(String kw) {
        if (kw == null) return false;
        String k = kw.toLowerCase().trim();

        if (k.length() < 3) return false;

        if (k.contains(".") || k.contains("/") || k.contains("\\") || k.contains(":")) return false; // likely path/package

        if (Set.of(
                "com", "org", "net", "io",
                "java", "jakarta", "javax",
                "exception", "error",
                "failed", "failure",
                "null", "instance",
                "bean", "type", "class", "method",
                "unable", "with", "from", "into", "before", "after"
        ).contains(k)) {
            return false;
        }

        if (k.matches(".*\\b(com|org|net|io)[a-z0-9]{8,}.*")) return false; // hash/noise

        if ((k.startsWith("com") || k.startsWith("org") || k.startsWith("net") || k.startsWith("io")) && k.contains("example")) {
            return false;
        }

        if (k.matches("^[0-9a-f]{8,}$")) return false;

        return true;
    }
}