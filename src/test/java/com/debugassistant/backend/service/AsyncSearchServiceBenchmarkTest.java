package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.github.GitHubIssue;
import com.debugassistant.backend.dto.stackoverflow.StackOverflowQuestion;
import com.debugassistant.backend.service.AsyncSearchService.SearchResults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Performance benchmark: sequential vs parallel API calls
 */

@ExtendWith(MockitoExtension.class)
class AsyncSearchServiceBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(AsyncSearchServiceBenchmarkTest.class);

    private static final int GITHUB_DELAY_MS        = 1_000;
    private static final int STACKOVERFLOW_DELAY_MS = 2_000;
    private static final int MARGIN_MS              =   400; // tolerated overshoot

    private static final List<String> QUERIES = List.of("NullPointerException java");

    @Mock private GitHubClient       gitHubClient;
    @Mock private StackOverflowClient stackOverflowClient;

    private AsyncSearchService asyncSearchService;

    @BeforeEach
    void setUp() {
        asyncSearchService = new AsyncSearchService(gitHubClient, stackOverflowClient);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void benchmark_sequential_takesApproximately3Seconds() {
        // Mock with delays
        when(gitHubClient.searchOnion(anyList())).thenAnswer(inv -> {
            sleep(GITHUB_DELAY_MS);
            return List.of(fakeIssue("GitHub result"));
        });
        when(stackOverflowClient.searchOnion(anyList(), anyString(), anyString())).thenAnswer(inv -> {
            sleep(STACKOVERFLOW_DELAY_MS);
            return List.of(fakeQuestion("StackOverflow result"));
        });

        long start = System.currentTimeMillis();

        // Sequential
        List<GitHubIssue>           ghResults = gitHubClient.searchOnion(QUERIES);
        List<StackOverflowQuestion> soResults = stackOverflowClient.searchOnion(QUERIES, "java", "NPE");

        long elapsed = System.currentTimeMillis() - start;
        int  expected = GITHUB_DELAY_MS + STACKOVERFLOW_DELAY_MS;

        log.info("Sequential benchmark: {} ms (expected ~{} ms)", elapsed, expected);

        assertThat(ghResults).hasSize(1);
        assertThat(soResults).hasSize(1);
        assertThat(elapsed)
                .as("Sequential should take at least %dms", expected)
                .isGreaterThanOrEqualTo(expected);
        assertThat(elapsed)
                .as("Sequential should finish within margin (%dms)", expected + MARGIN_MS)
                .isLessThan(expected + MARGIN_MS);
    }

    @Test
    @Timeout(value = 7, unit = TimeUnit.SECONDS)
    void benchmark_parallel_takesApproximately2Seconds() {
        when(gitHubClient.searchOnion(anyList())).thenAnswer(inv -> {
            sleep(GITHUB_DELAY_MS);
            return List.of(fakeIssue("GitHub result"));
        });
        when(stackOverflowClient.searchOnion(anyList(), anyString(), anyString())).thenAnswer(inv -> {
            sleep(STACKOVERFLOW_DELAY_MS);
            return List.of(fakeQuestion("StackOverflow result"));
        });

        long start = System.currentTimeMillis();

        SearchResults results = asyncSearchService.searchParallel(QUERIES, QUERIES, "java", "NPE");

        long elapsed = System.currentTimeMillis() - start;

        log.info("Parallel: {} ms (expected ~{}, bottleneck=SO)", elapsed, STACKOVERFLOW_DELAY_MS);

        assertThat(results.githubIssues()).hasSize(1);
        assertThat(results.soQuestions()).hasSize(1);
        assertThat(elapsed)
                .as("Parallel must not finish faster than the slowest call (%dms)", STACKOVERFLOW_DELAY_MS)
                .isGreaterThanOrEqualTo(STACKOVERFLOW_DELAY_MS);
        assertThat(elapsed)
                .as("Parallel should finish within slowest-call + margin (%dms)",
                        STACKOVERFLOW_DELAY_MS + MARGIN_MS)
                .isLessThan(STACKOVERFLOW_DELAY_MS + MARGIN_MS);
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void benchmark_printSummary() {
        // Sequential
        when(gitHubClient.searchOnion(anyList())).thenAnswer(inv -> {
            sleep(GITHUB_DELAY_MS);
            return List.of(fakeIssue("GitHub result"));
        });
        when(stackOverflowClient.searchOnion(anyList(), anyString(), anyString())).thenAnswer(inv -> {
            sleep(STACKOVERFLOW_DELAY_MS);
            return List.of(fakeQuestion("StackOverflow result"));
        });

        long seqStart = System.currentTimeMillis();
        gitHubClient.searchOnion(QUERIES);
        stackOverflowClient.searchOnion(QUERIES, "java", "NPE");
        long seqElapsed = System.currentTimeMillis() - seqStart;

        // Parallel
        long parStart = System.currentTimeMillis();
        asyncSearchService.searchParallel(QUERIES, QUERIES, "java", "NPE");
        long parElapsed = System.currentTimeMillis() - parStart;

        double speedup = (double) seqElapsed / parElapsed;
        long   saved   = seqElapsed - parElapsed;

        log.info("Benchmark: Sequential {} ms, Parallel {} ms, Saved {} ms",
                seqElapsed, parElapsed, saved);

        assertThat(parElapsed).isLessThan(seqElapsed);
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Benchmark sleep interrupted", e);
        }
    }

    private static GitHubIssue fakeIssue(String title) {
        return new GitHubIssue(title, "https://github.com/issue/1",
                "open", 0, null, Instant.now(), "body");
    }

    private static StackOverflowQuestion fakeQuestion(String title) {
        return new StackOverflowQuestion(1L, title, "https://stackoverflow.com/q/1",
                5, 2, true, Instant.now().getEpochSecond(), null);
    }
}
