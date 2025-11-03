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

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyzeServiceTest {

    @Mock
    private ParserRegistry parserRegistry;

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private RankingService rankingService;

    @InjectMocks
    private AnalyzeService analyzeService;

    @Test
    void shouldAnalyzeAndReturnResults() {
        String trace = "java.lang.NPE: null";
        ParsedError parsedError = new ParsedError("java", "NPE", "null");
        GitHubIssue issue = new GitHubIssue("Fix NPE", "url", "open", 2, null);

        when(parserRegistry.parse(trace)).thenReturn(parsedError);
        when(gitHubClient.searchIssues(anyString())).thenReturn(List.of(issue));
        when(rankingService.calculateScore(any())).thenReturn(5.0);

        AnalyzeResponse response = analyzeService.analyze(new AnalyzeRequest(trace));

        assertThat(response.getLanguage()).isEqualTo("java");
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getScore()).isEqualTo(5);

        verify(gitHubClient).searchIssues("NPE null");
    }

    @Test
    void shouldHandleEmptyGitHubResultsWithoutCrashing() {
        String trace = "Traceback...";
        ParsedError parsedError = new ParsedError("python", "Error", "msg");

        when(parserRegistry.parse(trace)).thenReturn(parsedError);
        when(gitHubClient.searchIssues(anyString())).thenReturn(Collections.emptyList());

        AnalyzeResponse response = analyzeService.analyze(new AnalyzeRequest(trace));

        assertThat(response.getResults()).isEmpty();
        assertThat(response.getScore()).isZero();
    }

    @Test
    void shouldSortResultsByScoreDescending() {
        String trace = "Error";
        ParsedError parsed = new ParsedError("java", "Err", "msg");

        GitHubIssue badIssue = new GitHubIssue("Bad", "url1", "open", 0, null);
        GitHubIssue goodIssue = new GitHubIssue("Good", "url2", "open", 5, null);

        when(parserRegistry.parse(anyString())).thenReturn(parsed);
        when(gitHubClient.searchIssues(anyString())).thenReturn(List.of(badIssue, goodIssue));

        when(rankingService.calculateScore(badIssue)).thenReturn(2.0);
        when(rankingService.calculateScore(goodIssue)).thenReturn(10.0);

        AnalyzeResponse response = analyzeService.analyze(new AnalyzeRequest(trace));

        assertThat(response.getResults()).hasSize(2);
        assertThat(response.getResults().get(0).getTitle()).isEqualTo("Good");
        assertThat(response.getResults().get(1).getTitle()).isEqualTo("Bad");

        assertThat(response.getScore()).isEqualTo(10);
    }
}