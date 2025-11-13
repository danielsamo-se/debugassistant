package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.github.GitHubIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Calculates relevance scores for GitHub issues.
 */
@Service
@Slf4j
public class RankingService {

    private static final double REACTIONS_WEIGHT = 0.5;
    private static final double KEYWORD_WEIGHT = 0.3;
    private static final double RECENCY_WEIGHT = 0.2;

    public double calculateScore(GitHubIssue issue, Set<String> keywords) {
        if (issue == null) return 0;

        double reactionScore = calcReactionScore(issue);
        double overlapScore = calcKeywordOverlap(issue, keywords);
        double recencyScore = calcRecencyScore(issue);

        double finalScore =
                REACTIONS_WEIGHT * reactionScore +
                        KEYWORD_WEIGHT * overlapScore +
                        RECENCY_WEIGHT * recencyScore;

        log.debug("Issue '{}' - reactions={}, overlap={}, recency={}, final={}",
                truncate(issue.title(), 30), reactionScore, overlapScore, recencyScore, finalScore);

        return finalScore;
    }

    private double calcReactionScore(GitHubIssue issue) {
        int reactions = 0;
        if (issue.reactions() != null && issue.reactions().totalCount() != null) {
            reactions = issue.reactions().totalCount();
        }

        int comments = issue.comments() != null ? issue.comments() : 0;
        int engagement = reactions + comments;

        // normalize: 20+ engagement = max score
        return Math.min(1.0, engagement / 20.0);
    }

    private double calcKeywordOverlap(GitHubIssue issue, Set<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return 0;

        String title = issue.title() != null ? issue.title().toLowerCase() : "";
        String body = issue.body() != null ? issue.body().toLowerCase() : "";
        String text = title + " " + body;

        long matches = keywords.stream()
                .map(String::toLowerCase)
                .filter(text::contains)
                .count();

        return (double) matches / keywords.size();
    }

    private double calcRecencyScore(GitHubIssue issue) {
        if (issue.createdAt() == null) return 0.5;

        long daysOld = ChronoUnit.DAYS.between(issue.createdAt(), Instant.now());

        if (daysOld <= 0) return 1.0;
        if (daysOld > 730) return 0.0; // older than 2 years

        // linear decay over 2 years
        return 1.0 - (daysOld / 730.0);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}