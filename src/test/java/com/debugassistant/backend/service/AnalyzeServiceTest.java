package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.AnalyzeRequest;
import com.debugassistant.backend.dto.AnalyzeResponse;
import com.debugassistant.backend.dto.github.GitHubIssue;
import com.debugassistant.backend.dto.stackoverflow.StackOverflowQuestion;
import com.debugassistant.backend.parser.ParsedError;
import com.debugassistant.backend.parser.ParserRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyzeServiceTest {

    @Mock
    private ParserRegistry parserRegistry;

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private StackOverflowClient stackOverflowClient;

    @Mock
    private RankingService rankingService;

    @Mock
    private QueryBuilder queryBuilder;

    @InjectMocks
    private AnalyzeService analyzeService;

    // === Existing Tests (updated for new structure) ===

    @Test
    void shouldAnalyzeAndReturnResults() {
        String trace = "java.lang.NPE: null";

        ParsedError parsedError = ParsedError.builder()
                .language("java")
                .exceptionType("NPE")
                .message("null")
                .keywords(Set.of("npe"))
                .build();

        GitHubIssue issue = new GitHubIssue(
                "Fix NPE",
                "url",
                "open",
                2,
                null,
                Instant.now(),
                "some body"
        );

        when(parserRegistry.parse(trace)).thenReturn(parsedError);
        when(queryBuilder.buildSmartQuery(parsedError)).thenReturn("built-query");
        when(gitHubClient.searchIssues("built-query")).thenReturn(List.of(issue));
        when(stackOverflowClient.search("built-query", "java")).thenReturn(List.of());
        when(rankingService.calculateGitHubScore(eq(issue), anySet())).thenReturn(5.0);

        AnalyzeResponse response = analyzeService.analyze(new AnalyzeRequest(trace));

        assertThat(response.language()).isEqualTo("java");
        assertThat(response.results()).hasSize(1);
        assertThat(response.results().getFirst().title()).isEqualTo("Fix NPE");

        verify(parserRegistry).parse(trace);
        verify(queryBuilder).buildSmartQuery(parsedError);
        verify(gitHubClient).searchIssues("built-query");
        verify(rankingService).calculateGitHubScore(eq(issue), anySet());
    }

    @Test
    void shouldHandleEmptyGitHubResults() {
        String trace = "Traceback...";
        ParsedError parsedError = ParsedError.builder()
                .language("python")
                .exceptionType("Error")
                .message("msg")
                .keywords(Set.of())
                .build();

        when(parserRegistry.parse(trace)).thenReturn(parsedError);
        when(queryBuilder.buildSmartQuery(parsedError)).thenReturn("built-query");
        when(gitHubClient.searchIssues("built-query")).thenReturn(Collections.emptyList());
        when(stackOverflowClient.search("built-query", "python")).thenReturn(Collections.emptyList());

        AnalyzeResponse response = analyzeService.analyze(new AnalyzeRequest(trace));

        assertThat(response.results()).isEmpty();
        verify(rankingService, never()).calculateGitHubScore(any(), anySet());
        verify(rankingService, never()).calculateStackOverflowScore(any(), anySet());
    }

    @Test
    void shouldSortResultsByScoreDescending() {
        String trace = "Error";
        ParsedError parsed = ParsedError.builder()
                .language("java")
                .exceptionType("Err")
                .message("msg")
                .keywords(Set.of("err"))
                .build();

        GitHubIssue lowIssue = new GitHubIssue(
                "Low",
                "url1",
                "open",
                0,
                null,
                Instant.now().minusSeconds(3600),
                "body1"
        );

        GitHubIssue highIssue = new GitHubIssue(
                "High",
                "url2",
                "open",
                5,
                null,
                Instant.now(),
                "body2"
        );

        when(parserRegistry.parse(anyString())).thenReturn(parsed);
        when(queryBuilder.buildSmartQuery(parsed)).thenReturn("built-query");
        when(gitHubClient.searchIssues("built-query")).thenReturn(List.of(lowIssue, highIssue));
        when(stackOverflowClient.search("built-query", "java")).thenReturn(List.of());
        when(rankingService.calculateGitHubScore(eq(lowIssue), anySet())).thenReturn(2.0);
        when(rankingService.calculateGitHubScore(eq(highIssue), anySet())).thenReturn(10.0);

        AnalyzeResponse response = analyzeService.analyze(new AnalyzeRequest(trace));

        assertThat(response.results()).hasSize(2);
        assertThat(response.results().get(0).title()).isEqualTo("High");
        assertThat(response.results().get(1).title()).isEqualTo("Low");
    }

    // === New Stack Overflow Tests ===

    @Test
    void shouldMergeResultsFromBothSources() {
        String trace = "java.lang.NPE: null";

        ParsedError parsedError = ParsedError.builder()
                .language("java")
                .exceptionType("NPE")
                .message("null")
                .keywords(Set.of("npe"))
                .build();

        GitHubIssue githubIssue = new GitHubIssue(
                "GitHub Fix", "url1", "open", 2, null, Instant.now(), "body"
        );

        StackOverflowQuestion soQuestion = new StackOverflowQuestion(
                1L, "SO Fix", "url2", 10, 3, true, Instant.now().getEpochSecond(), null
        );

        when(parserRegistry.parse(trace)).thenReturn(parsedError);
        when(queryBuilder.buildSmartQuery(parsedError)).thenReturn("query");
        when(gitHubClient.searchIssues("query")).thenReturn(List.of(githubIssue));
        when(stackOverflowClient.search("query", "java")).thenReturn(List.of(soQuestion));
        when(rankingService.calculateGitHubScore(any(), anySet())).thenReturn(5.0);
        when(rankingService.calculateStackOverflowScore(any(), anySet())).thenReturn(8.0);

        AnalyzeResponse response = analyzeService.analyze(new AnalyzeRequest(trace));

        assertThat(response.results()).hasSize(2);
        assertThat(response.results().get(0).source()).isEqualTo("stackoverflow");
        assertThat(response.results().get(1).source()).isEqualTo("github");
    }

    @Test
    void shouldIncludeStackOverflowSpecificFields() {
        String trace = "error";
        ParsedError parsed = ParsedError.builder()
                .language("python")
                .exceptionType("ValueError")
                .message("invalid")
                .keywords(Set.of("valueerror"))
                .build();

        StackOverflowQuestion question = new StackOverflowQuestion(
                1L, "Python ValueError", "url", 15, 7, true,
                Instant.now().getEpochSecond(), null
        );

        when(parserRegistry.parse(trace)).thenReturn(parsed);
        when(queryBuilder.buildSmartQuery(parsed)).thenReturn("query");
        when(gitHubClient.searchIssues("query")).thenReturn(List.of());
        when(stackOverflowClient.search("query", "python")).thenReturn(List.of(question));
        when(rankingService.calculateStackOverflowScore(any(), anySet())).thenReturn(7.0);

        AnalyzeResponse response = analyzeService.analyze(new AnalyzeRequest(trace));

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).answerCount()).isEqualTo(7);
        assertThat(response.results().get(0).isAnswered()).isTrue();
    }

    @Test
    void shouldLimitResultsToFifteen() {
        String trace = "error";
        ParsedError parsed = ParsedError.builder()
                .language("java")
                .exceptionType("Exception")
                .message("msg")
                .keywords(Set.of())
                .build();

        List<GitHubIssue> manyIssues = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            manyIssues.add(new GitHubIssue(
                    "Issue " + i, "url" + i, "open", i, null, Instant.now(), "body"
            ));
        }

        when(parserRegistry.parse(trace)).thenReturn(parsed);
        when(queryBuilder.buildSmartQuery(parsed)).thenReturn("query");
        when(gitHubClient.searchIssues("query")).thenReturn(manyIssues);
        when(stackOverflowClient.search("query", "java")).thenReturn(List.of());
        when(rankingService.calculateGitHubScore(any(), anySet())).thenReturn(5.0);

        AnalyzeResponse response = analyzeService.analyze(new AnalyzeRequest(trace));

        assertThat(response.results()).hasSize(15);
    }
}