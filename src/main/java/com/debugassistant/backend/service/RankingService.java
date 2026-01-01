package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.github.GitHubIssue;
import com.debugassistant.backend.dto.stackoverflow.StackOverflowQuestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calculates relevance scores for search results
 */
@Service
@Slf4j
public class RankingService {

    private static final double REACTIONS_WEIGHT = 0.25;    // engagement signal
    private static final double KEYWORD_WEIGHT = 0.40;      // semantic relevance
    private static final double RECENCY_WEIGHT = 0.10;      // freshness tie-breaker
    private static final double TITLE_MATCH_BONUS = 0.15;   // strong intent in title

    private static final Set<String> TRUSTED_REPO_KEYWORDS = Set.of(
            "hibernate", "spring-projects", "spring-boot", "spring-framework",
            "quarkus", "jakarta", "eclipse", "apache", "jpa", "persistence"
    ); // ecosystem bias

    public double calculateGitHubScore(GitHubIssue issue, Set<String> keywords) {
        if (issue == null) return -1.0; // invalid item

        if (issue.title() != null && !issue.title().matches(".*[a-zA-Z]{3,}.*")) {
            return -1.0; // low-signal title
        }

        Instant now = Instant.now(); // scoring reference

        String titleLower = (issue.title() != null ? issue.title() : "").toLowerCase();
        String bodyLower = (issue.body() != null ? issue.body() : "").toLowerCase();
        String haystack = titleLower + " " + bodyLower; // match surface

        Set<String> exceptionKeywords = (keywords == null || keywords.isEmpty())
                ? Set.of()
                : keywords.stream()
                .map(k -> k == null ? "" : k.toLowerCase().trim())
                .filter(k -> !k.isBlank())
                .filter(k -> k.contains("exception") || k.contains("error"))
                .collect(Collectors.toSet()); // hard anchors

        boolean exceptionInTitle = exceptionKeywords.stream()
                .anyMatch(titleLower::contains); // title intent

        if (!exceptionInTitle && !exceptionKeywords.isEmpty()) {
            return -1.0; // mismatch guard
        }

        Set<String> anchors = (keywords == null || keywords.isEmpty())
                ? Set.of()
                : keywords.stream()
                .map(k -> k == null ? "" : k.toLowerCase().trim())
                .filter(k -> !k.isBlank())
                .filter(this::isStrongKeyword)
                .collect(Collectors.toSet()); // high-signal tokens

        long anchorHits = anchors.stream().filter(haystack::contains).count(); // overlap count

        if (!anchors.isEmpty() && (double) anchorHits / anchors.size() < 0.5) {
            return -1.0; // precision cutoff
        }

        double reactionScore = calcGitHubReactionScore(issue);                 // engagement
        double overlapScore = (anchors.isEmpty()) ? 0 : ((double) anchorHits / anchors.size()); // token overlap
        double recencyScore = calcRecencyScore(issue.createdAt(), now);        // freshness
        double titleBonus = exceptionInTitle ? TITLE_MATCH_BONUS : 0.0;        // title boost
        double repoBonus = calcRepoBonus(issue.htmlUrl());                     // trusted repo bias

        double finalScore = REACTIONS_WEIGHT * reactionScore +
                KEYWORD_WEIGHT * overlapScore +
                RECENCY_WEIGHT * recencyScore +
                titleBonus +
                repoBonus; // weighted mix

        log.debug("GitHub '{}' - reactions={}, overlap={}, recency={}, titleBonus={}, repoBonus={}, final={}",
                truncate(issue.title(), 30), reactionScore, overlapScore, recencyScore, titleBonus, repoBonus, finalScore);

        return finalScore;
    }

    public double calculateStackOverflowScore(StackOverflowQuestion question, Set<String> keywords) {
        if (question == null) return 0;

        Instant now = Instant.now(); // scoring reference

        Set<String> used = (keywords == null || keywords.isEmpty())
                ? Set.of()
                : keywords.stream().map(String::toLowerCase).collect(Collectors.toSet()); // normalized keywords

        double overlapScore = calcKeywordOverlap(question.title(), null, used); // title-only overlap
        double reactionScore = calcStackOverflowReactionScore(question);        // engagement
        double recencyScore = calcRecencyFromEpoch(question.creationDate(), now); // freshness
        double sourceScore = question.isAnswered() ? 1.0 : 0.5;                 // answered preference
        double acceptedBonus = question.isAnswered() && question.answerCount() > 0 ? 0.10 : 0.0; // solved bonus

        return 0.20 * reactionScore +
                0.40 * overlapScore +
                0.10 * recencyScore +
                0.20 * sourceScore +
                acceptedBonus; // weighted mix
    }

    private double calcGitHubReactionScore(GitHubIssue issue) {
        int reactions = 0;
        if (issue.reactions() != null && issue.reactions().totalCount() != null) {
            reactions = issue.reactions().totalCount();
        }
        int comments = issue.comments() != null ? issue.comments() : 0;
        int engagement = reactions + comments; // combined engagement
        if (engagement == 0) return 0.0;      // no signal
        if (engagement < 3) return 0.1;       // minimal credit
        return Math.min(1.0, engagement / 20.0); // saturation cap
    }

    private double calcStackOverflowReactionScore(StackOverflowQuestion question) {
        int score = Math.max(0, question.score()); // ignore negatives
        int answers = question.answerCount();
        int engagement = score + (answers * 3);    // answers > votes
        return Math.min(1.0, engagement / 100.0);
    }

    private double calcKeywordOverlap(String title, String body, Set<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return 0; // no anchors

        String text = (title != null ? title.toLowerCase() : "") +
                " " + (body != null ? body.toLowerCase() : ""); // match surface

        long matches = keywords.stream()
                .map(String::toLowerCase)
                .filter(text::contains)
                .count();

        return (double) matches / keywords.size(); // normalized overlap
    }

    private double calcRecencyScore(Instant createdAt, Instant now) {
        if (createdAt == null) return 0.5; // fallback
        long daysOld = ChronoUnit.DAYS.between(createdAt, now);
        if (daysOld <= 0) return 1.0;
        if (daysOld > 730) return 0.0;
        return 1.0 - (daysOld / 730.0);
    }

    private double calcRecencyFromEpoch(Long epochSeconds, Instant now) {
        if (epochSeconds == null) return 0.5; // fallback
        Instant createdAt = Instant.ofEpochSecond(epochSeconds);
        return calcRecencyScore(createdAt, now); // reuse decay
    }

    private double calcRepoBonus(String htmlUrl) {
        if (htmlUrl == null) return 0.0; // no url
        String urlLower = htmlUrl.toLowerCase();
        boolean trusted = TRUSTED_REPO_KEYWORDS.stream().anyMatch(urlLower::contains); // trusted orgs/libs
        return trusted ? 0.10 : 0.0; // bias boost
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return ""; // log safety
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    private boolean isStrongKeyword(String k) {
        if (k == null) return false;

        if (k.length() < 4) return false; // short noise

        if (k.matches("^\\d+$")) return false;       // numeric noise
        if (k.matches("^[^a-zA-Z]+$")) return false; // symbol noise

        if (Set.of(
                "java", "error", "exception", "failed", "failure", "null",
                "instance", "bean", "with", "from", "that", "this", "type", "value"
        ).contains(k)) return false; // stop tokens

        if (k.matches(".*\\b(com|org|net|io)[a-z0-9]{6,}.*")) return false; // package-like noise

        if (k.matches("^[0-9a-f]{8,}$")) return false; // hash-like noise

        return true;
    }
}

