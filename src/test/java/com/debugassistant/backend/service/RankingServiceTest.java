package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.github.GitHubIssue;
import com.debugassistant.backend.dto.github.GitHubIssue.Reactions;
import com.debugassistant.backend.dto.stackoverflow.StackOverflowQuestion;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RankingServiceTest {

    private final RankingService ranking = new RankingService();

    @Test
    void reactionScoreIsNormalizedCorrectly() {
        GitHubIssue issue = new GitHubIssue(
                "title",
                "url",
                "open",
                10,
                new Reactions(4),
                Instant.now(),
                ""
        );

        double score = ranking.calculateGitHubScore(issue, Set.of());

        assertThat(score).isGreaterThan(0.3);
        assertThat(score).isLessThan(0.8);
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

        double score = ranking.calculateGitHubScore(issue, keywords);

        assertThat(score).isGreaterThan(0.1);
        assertThat(score).isLessThan(0.7);
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
                Instant.now(),
                "body"
        );

        double oldScore = ranking.calculateGitHubScore(oldIssue, Set.of());
        double newScore = ranking.calculateGitHubScore(newIssue, Set.of());

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

        double score = ranking.calculateGitHubScore(issue, keywords);

        assertThat(score).isGreaterThan(0.3);
        assertThat(score).isLessThan(1.0);
    }

    @Test
    void shouldScoreStackOverflowHigherWhenAnswered() {
        StackOverflowQuestion answered = new StackOverflowQuestion(
                1L, "How to fix NPE", "https://so.com/1", 20, 3, true,
                Instant.now().getEpochSecond(), null
        );

        StackOverflowQuestion unanswered = new StackOverflowQuestion(
                2L, "How to fix NPE", "https://so.com/2", 20, 3, false,
                Instant.now().getEpochSecond(), null
        );

        Set<String> keywords = Set.of("npe");

        double answeredScore = ranking.calculateStackOverflowScore(answered, keywords);
        double unansweredScore = ranking.calculateStackOverflowScore(unanswered, keywords);

        assertThat(answeredScore).isGreaterThan(unansweredScore);
    }

    @Test
    void shouldScoreStackOverflowWithHighVotes() {
        StackOverflowQuestion highVotes = new StackOverflowQuestion(
                1L, "Popular question", "https://so.com/1", 100, 10, true,
                Instant.now().getEpochSecond(), null
        );

        StackOverflowQuestion lowVotes = new StackOverflowQuestion(
                2L, "Unpopular question", "https://so.com/2", 2, 1, true,
                Instant.now().getEpochSecond(), null
        );

        double highScore = ranking.calculateStackOverflowScore(highVotes, Set.of());
        double lowScore = ranking.calculateStackOverflowScore(lowVotes, Set.of());

        assertThat(highScore).isGreaterThan(lowScore);
    }

    @Test
    void shouldHandleNullGitHubIssue() {
        double score = ranking.calculateGitHubScore(null, Set.of("test"));
        assertThat(score).isZero();
    }

    @Test
    void shouldHandleNullStackOverflowQuestion() {
        double score = ranking.calculateStackOverflowScore(null, Set.of("test"));
        assertThat(score).isZero();
    }

    @Test
    void shouldHandleEmptyKeywordsForStackOverflow() {
        StackOverflowQuestion question = new StackOverflowQuestion(
                1L, "Some question", "url", 10, 2, true,
                Instant.now().getEpochSecond(), null
        );

        double score = ranking.calculateStackOverflowScore(question, Set.of());
        assertThat(score).isGreaterThanOrEqualTo(0);
    }
}