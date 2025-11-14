package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.github.GitHubIssue;
import com.debugassistant.backend.dto.stackoverflow.StackOverflowQuestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Calculates relevance scores for search results
 */
@Service
@Slf4j
public class RankingService {

    // weights chosen for balance
    private static final double REACTIONS_WEIGHT = 0.4;
    private static final double KEYWORD_WEIGHT = 0.3;
    private static final double RECENCY_WEIGHT = 0.15;
    private static final double SOURCE_WEIGHT = 0.15;

    public double calculateGitHubScore(GitHubIssue issue, Set<String> keywords) {
        if (issue == null) return 0;

        double reactionScore = calcGitHubReactionScore(issue);
        double overlapScore = calcKeywordOverlap(issue.title(), issue.body(), keywords);
        double recencyScore = calcRecencyScore(issue.createdAt());
        double sourceScore = 0.5; // simple default source score

        double finalScore = REACTIONS_WEIGHT * reactionScore +
                KEYWORD_WEIGHT * overlapScore +
                RECENCY_WEIGHT * recencyScore +
                SOURCE_WEIGHT * sourceScore;

        log.debug("GitHub '{}' - reactions={}, overlap={}, recency={}, final={}",
                truncate(issue.title(), 30), reactionScore, overlapScore, recencyScore, finalScore);

        return finalScore;
    }

    public double calculateStackOverflowScore(StackOverflowQuestion question, Set<String> keywords) {
        if (question == null) return 0;

        double reactionScore = calcStackOverflowReactionScore(question);
        double overlapScore = calcKeywordOverlap(question.title(), null, keywords);
        double recencyScore = calcRecencyFromEpoch(question.creationDate());
        double sourceScore = question.isAnswered() ? 1.0 : 0.7;

        double finalScore = REACTIONS_WEIGHT * reactionScore +
                KEYWORD_WEIGHT * overlapScore +
                RECENCY_WEIGHT * recencyScore +
                SOURCE_WEIGHT * sourceScore;

        log.debug("SO '{}' - reactions={}, overlap={}, recency={}, answered={}, final={}",
                truncate(question.title(), 30), reactionScore, overlapScore, recencyScore, question.isAnswered(), finalScore);

        return finalScore;
    }

    private double calcGitHubReactionScore(GitHubIssue issue) {
        int reactions = 0;
        if (issue.reactions() != null && issue.reactions().totalCount() != null) {
            reactions = issue.reactions().totalCount();
        }
        int comments = issue.comments() != null ? issue.comments() : 0;
        int engagement = reactions + comments;
        return Math.min(1.0, engagement / 20.0); // scale engagement to 1
    }

    private double calcStackOverflowReactionScore(StackOverflowQuestion question) {
        int score = Math.max(0, question.score());
        int answers = question.answerCount();
        int engagement = score + (answers * 2);
        return Math.min(1.0, engagement / 25.0); // same scaling idea here
    }

    private double calcKeywordOverlap(String title, String body, Set<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return 0;

        String text = (title != null ? title.toLowerCase() : "") +
                " " + (body != null ? body.toLowerCase() : "");

        long matches = keywords.stream()
                .map(String::toLowerCase)
                .filter(text::contains)
                .count();

        return (double) matches / keywords.size(); // measure keyword match ratio
    }

    private double calcRecencyScore(Instant createdAt) {
        if (createdAt == null) return 0.5; // use neutral score here
        long daysOld = ChronoUnit.DAYS.between(createdAt, Instant.now());
        if (daysOld <= 0) return 1.0;
        if (daysOld > 730) return 0.0; // older than two years
        return 1.0 - (daysOld / 730.0);
    }

    private double calcRecencyFromEpoch(Long epochSeconds) {
        if (epochSeconds == null) return 0.5;
        Instant createdAt = Instant.ofEpochSecond(epochSeconds);
        return calcRecencyScore(createdAt);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text; // shorten text for logging
    }

    public double calculateScore(GitHubIssue issue, Set<String> keywords) {
        return calculateGitHubScore(issue, keywords);
    }
}