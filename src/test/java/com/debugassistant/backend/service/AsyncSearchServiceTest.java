package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.github.GitHubIssue;
import com.debugassistant.backend.dto.stackoverflow.StackOverflowQuestion;
import com.debugassistant.backend.service.AsyncSearchService.SearchResults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncSearchServiceTest {

    @Mock private GitHubClient gitHubClient;
    @Mock private StackOverflowClient stackOverflowClient;

    @InjectMocks private AsyncSearchService asyncSearchService;

    private static final List<String> GH_QUERIES = List.of("gh-query");
    private static final List<String> SO_QUERIES = List.of("so-query");

    private static GitHubIssue issue(String title) {
        return new GitHubIssue(title, "https://github.com/issue/1", "open", 3, null, Instant.now(), "body");
    }

    private static StackOverflowQuestion question(String title) {
        return new StackOverflowQuestion(1L, title, "https://stackoverflow.com/q/1",
                5, 2, true, Instant.now().getEpochSecond(), null);
    }

    @Test
    void searchParallel_returnsBothResults() {
        GitHubIssue ghIssue = issue("Fix NPE");
        StackOverflowQuestion soQuestion = question("How to fix NPE");

        when(gitHubClient.searchOnion(GH_QUERIES)).thenReturn(List.of(ghIssue));
        when(stackOverflowClient.searchOnion(SO_QUERIES, "java", "NPE")).thenReturn(List.of(soQuestion));

        SearchResults results = asyncSearchService.searchParallel(GH_QUERIES, SO_QUERIES, "java", "NPE");

        assertThat(results.githubIssues()).containsExactly(ghIssue);
        assertThat(results.soQuestions()).containsExactly(soQuestion);
    }

    @Test
    void searchParallel_withEmptyResults_returnsTwoEmptyLists() {
        when(gitHubClient.searchOnion(anyList())).thenReturn(List.of());
        when(stackOverflowClient.searchOnion(anyList(), anyString(), anyString())).thenReturn(List.of());

        SearchResults results = asyncSearchService.searchParallel(GH_QUERIES, SO_QUERIES, "java", "NPE");

        assertThat(results.githubIssues()).isEmpty();
        assertThat(results.soQuestions()).isEmpty();
    }

    @Test
    void searchParallel_withMultipleResults_preservesAllItems() {
        List<GitHubIssue> issues = List.of(issue("A"), issue("B"), issue("C"));
        List<StackOverflowQuestion> questions = List.of(question("X"), question("Y"));

        when(gitHubClient.searchOnion(anyList())).thenReturn(issues);
        when(stackOverflowClient.searchOnion(anyList(), anyString(), anyString())).thenReturn(questions);

        SearchResults results = asyncSearchService.searchParallel(GH_QUERIES, SO_QUERIES, "java", "NPE");

        assertThat(results.githubIssues()).hasSize(3).containsExactlyElementsOf(issues);
        assertThat(results.soQuestions()).hasSize(2).containsExactlyElementsOf(questions);
    }

    @Test
    void searchParallel_passesCorrectQueriesToEachClient() {
        List<String> ghQueries = List.of("q1", "q2", "q3");
        List<String> soQueries = List.of("so1", "so2");

        when(gitHubClient.searchOnion(ghQueries)).thenReturn(List.of());
        when(stackOverflowClient.searchOnion(soQueries, "python", "KeyError")).thenReturn(List.of());

        asyncSearchService.searchParallel(ghQueries, soQueries, "python", "KeyError");

        verify(gitHubClient).searchOnion(ghQueries);
        verify(stackOverflowClient).searchOnion(soQueries, "python", "KeyError");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void searchParallel_executesBothSearchesConcurrently() throws InterruptedException {
        CountDownLatch bothStarted = new CountDownLatch(2);
        AtomicBoolean ghWasConcurrent = new AtomicBoolean(false);
        AtomicBoolean soWasConcurrent = new AtomicBoolean(false);

        when(gitHubClient.searchOnion(anyList())).thenAnswer(inv -> {
            bothStarted.countDown();                              // signal "I've started"
            ghWasConcurrent.set(bothStarted.await(2, TimeUnit.SECONDS)); // wait for SO to also start
            return List.of();
        });

        when(stackOverflowClient.searchOnion(anyList(), anyString(), anyString())).thenAnswer(inv -> {
            bothStarted.countDown();
            soWasConcurrent.set(bothStarted.await(2, TimeUnit.SECONDS));
            return List.of();
        });

        asyncSearchService.searchParallel(GH_QUERIES, SO_QUERIES, "java", "NPE");

        assertThat(ghWasConcurrent.get())
                .as("GitHub search must start while StackOverflow search is already running")
                .isTrue();
        assertThat(soWasConcurrent.get())
                .as("StackOverflow search must start while GitHub search is already running")
                .isTrue();
    }

    @Test
    void searchGitHubAsync_returnsGitHubResults() {
        GitHubIssue ghIssue = issue("Fix");
        when(gitHubClient.searchOnion(GH_QUERIES)).thenReturn(List.of(ghIssue));

        List<GitHubIssue> result = asyncSearchService.searchGitHubAsync(GH_QUERIES).join();

        assertThat(result).containsExactly(ghIssue);
        verify(gitHubClient).searchOnion(GH_QUERIES);
    }

    @Test
    void searchStackOverflowAsync_returnsSOResults() {
        StackOverflowQuestion soQuestion = question("Fix");
        when(stackOverflowClient.searchOnion(SO_QUERIES, "python", "ValueError"))
                .thenReturn(List.of(soQuestion));

        List<StackOverflowQuestion> result = asyncSearchService
                .searchStackOverflowAsync(SO_QUERIES, "python", "ValueError").join();

        assertThat(result).containsExactly(soQuestion);
        verify(stackOverflowClient).searchOnion(SO_QUERIES, "python", "ValueError");
    }

    @Test
    void searchParallel_whenGitHubFails_throwsCompletionException() {
        when(gitHubClient.searchOnion(anyList()))
                .thenThrow(new RuntimeException("GitHub rate limit exceeded"));
        when(stackOverflowClient.searchOnion(anyList(), anyString(), anyString()))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                asyncSearchService.searchParallel(GH_QUERIES, SO_QUERIES, "java", "NPE"))
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("GitHub rate limit exceeded");
    }

    @Test
    void searchParallel_whenStackOverflowFails_throwsCompletionException() {
        when(gitHubClient.searchOnion(anyList())).thenReturn(List.of());
        when(stackOverflowClient.searchOnion(anyList(), anyString(), anyString()))
                .thenThrow(new RuntimeException("StackOverflow connection timeout"));

        assertThatThrownBy(() ->
                asyncSearchService.searchParallel(GH_QUERIES, SO_QUERIES, "java", "NPE"))
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("StackOverflow connection timeout");
    }

    @Test
    void searchGitHubAsync_whenClientThrows_futureCompletesExceptionally() {
        when(gitHubClient.searchOnion(anyList()))
                .thenThrow(new RuntimeException("network error"));

        assertThatThrownBy(() -> asyncSearchService.searchGitHubAsync(GH_QUERIES).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("network error");
    }

    @Test
    void searchStackOverflowAsync_whenClientThrows_futureCompletesExceptionally() {
        when(stackOverflowClient.searchOnion(anyList(), anyString(), anyString()))
                .thenThrow(new RuntimeException("API error"));

        assertThatThrownBy(() ->
                asyncSearchService.searchStackOverflowAsync(SO_QUERIES, "java", "NPE").join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("API error");
    }
}
