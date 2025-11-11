package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.github.GitHubIssue;
import com.debugassistant.backend.dto.github.GitHubIssue.Reactions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RankingServiceTest {

    private final RankingService ranking = new RankingService();

    @Test
    void reactionScoreIsNormalizedCorrectly() {
        RankingService service = new RankingService();

        GitHubIssue issue = new GitHubIssue(
                "title",
                "url",
                "open",
                10,
                new GitHubIssue.Reactions(4), // 14 engagement
                Instant.now(),
                ""
        );

        double score = service.calculateScore(issue, Set.of());

        // final = 0.5 * 0.7 + 0.3*overlap(=0) + 0.2*recency(=1)
        // recency=1 if createdAt=now()
        // => final = 0.35 + 0.2 = 0.55

        assertThat(score).isEqualTo(0.55);
    }

    @Test
    void keywordOverlapIsCalculatedCorrectly() {
        GitHubIssue issue = new GitHubIssue(
                "Database connection failed",
                "url",
                "open",
                0,
                null,
                Instant.now(),
                "Timeout happened due to connection issues"
        );

        Set<String> keywords = Set.of("connection", "timeout", "nullpointer");

        double score = ranking.calculateScore(issue, keywords);

        assertThat(score).isGreaterThan(0.1);
        assertThat(score).isLessThan(0.5);
    }

    @Test
    void recencyScoreGivesHigherWeightToNewerIssues() {
        GitHubIssue oldIssue = new GitHubIssue(
                "Old",
                "url",
                "open",
                0,
                null,
                Instant.now().minusSeconds(365 * 24 * 3600),
                "body"
        );

        GitHubIssue newIssue = new GitHubIssue(
                "New",
                "url",
                "open",
                0,
                null,
                Instant.now(), // now
                "body"
        );

        double oldScore = ranking.calculateScore(oldIssue, Set.of());
        double newScore = ranking.calculateScore(newIssue, Set.of());

        assertThat(newScore).isGreaterThan(oldScore);
    }

    @Test
    void finalScoreCombinesAllComponents() {
        GitHubIssue issue = new GitHubIssue(
                "Connection timeout",
                "url",
                "open",
                3,
                new Reactions(7),
                Instant.now().minusSeconds(24 * 3600),
                "Happens when server does not respond"
        );

        Set<String> keywords = Set.of("timeout", "server");

        double score = ranking.calculateScore(issue, keywords);

        assertThat(score).isGreaterThan(0.3);
        assertThat(score).isLessThan(1.0);
    }
}