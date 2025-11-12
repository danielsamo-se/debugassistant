package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.github.GitHubIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Ranks GitHub issues based on simple relevance signals
 */

@Service
@Slf4j
public class RankingService {

    private static final double REACTIONS_WEIGHT = 0.5;
    private static final double KEYWORD_WEIGHT = 0.3;
    private static final double RECENCY_WEIGHT = 0.2;

    /**
     * Calculates how relevant a GitHub issue is for the user's error.
     */
    public double calculateScore(GitHubIssue issue, Set<String> errorKeywords) {
        if (issue == null) return 0;

        double reactionScore = calcReactionScore(issue);
        double overlapScore = calcKeywordOverlap(issue, errorKeywords);
        double recencyScore = calcRecency(issue);

        double finalScore =
                REACTIONS_WEIGHT * reactionScore +
                        KEYWORD_WEIGHT * overlapScore +
                        RECENCY_WEIGHT * recencyScore;

        log.debug("Score breakdown: reactions={}, overlap={}, recency={}, final={}",
                reactionScore, overlapScore, recencyScore, finalScore);

        return finalScore;
    }

    /**
     * Converts reactions and comments into a normalized score between 0 and 1.
     */
    private double calcReactionScore(GitHubIssue issue) {
        int reactions = issue.reactions() != null && issue.reactions().totalCount() != null
                ? issue.reactions().totalCount()
                : 0;

        int comments = issue.comments() != null ? issue.comments() : 0;

        int engagement = reactions + comments;

        return Math.min(1.0, engagement / 20.0);
    }

    /**
     * Measures engagement through reactions and comments.
     */
    private double calcKeywordOverlap(GitHubIssue issue, Set<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return 0;

        String text = (issue.title() + " " + (issue.body() != null ? issue.body() : ""))
                .toLowerCase();

        long matches = keywords.stream()
                .filter(text::contains)
                .count();

        return (double) matches / keywords.size();
    }

    /**
     * Newer issues get a slightly higher score.
     */
    private double calcRecency(GitHubIssue issue) {
        if (issue.createdAt() == null) return 0;

        long daysOld = ChronoUnit.DAYS.between(issue.createdAt(), Instant.now());

        if (daysOld <= 0) return 1.0;
        if (daysOld > 365) return 0.0;

        // yearly decay
        return 1.0 - (daysOld / 365.0);
    }
}