package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.AnalyzeRequest;
import com.debugassistant.backend.dto.AnalyzeResponse;
import com.debugassistant.backend.dto.github.GitHubIssue;
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
    private RankingService rankingService;

    @Mock
    private QueryBuilder queryBuilder;

    @InjectMocks
    private AnalyzeService analyzeService;

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
        when(rankingService.calculateScore(eq(issue), anySet())).thenReturn(5.0);

        AnalyzeResponse response = analyzeService.analyze(new AnalyzeRequest(trace));

        assertThat(response.language()).isEqualTo("java");
        assertThat(response.results()).hasSize(1);
        assertThat(response.results().getFirst().title()).isEqualTo("Fix NPE");

        verify(parserRegistry).parse(trace);
        verify(queryBuilder).buildSmartQuery(parsedError);
        verify(gitHubClient).searchIssues("built-query");
        verify(rankingService).calculateScore(eq(issue), anySet());
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

        AnalyzeResponse response = analyzeService.analyze(new AnalyzeRequest(trace));

        assertThat(response.results()).isEmpty();
        verify(rankingService, never()).calculateScore(any(), anySet());
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
        when(rankingService.calculateScore(eq(lowIssue), anySet())).thenReturn(2.0);
        when(rankingService.calculateScore(eq(highIssue), anySet())).thenReturn(10.0);

        AnalyzeResponse response = analyzeService.analyze(new AnalyzeRequest(trace));

        assertThat(response.results()).hasSize(2);
        assertThat(response.results().get(0).title()).isEqualTo("High");
        assertThat(response.results().get(1).title()).isEqualTo("Low");
    }
}