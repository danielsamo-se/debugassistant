package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.github.GitHubIssue;
import com.debugassistant.backend.dto.stackoverflow.StackOverflowQuestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Parallel GitHub and StackOverflow search using virtual threads
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncSearchService {

    // Executor for parallel searches
    private final Executor searchExecutor =
            Executors.newThreadPerTaskExecutor(
                    Thread.ofVirtual().name("search-", 0).factory()
            );

    private final GitHubClient gitHubClient;
    private final StackOverflowClient stackOverflowClient;

    public record SearchResults(
            List<GitHubIssue> githubIssues,
            List<StackOverflowQuestion> soQuestions
    ) {}

    public CompletableFuture<List<GitHubIssue>> searchGitHubAsync(List<String> queries) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("GitHub search started (thread={})", Thread.currentThread().getName());
            List<GitHubIssue> results = gitHubClient.searchOnion(queries);
            log.debug("GitHub search done — {} results", results.size());
            return results;
        }, searchExecutor);
    }

    public CompletableFuture<List<StackOverflowQuestion>> searchStackOverflowAsync(
            List<String> queries,
            String language,
            String exceptionType
    ) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("StackOverflow search started (thread={})", Thread.currentThread().getName());
            List<StackOverflowQuestion> results =
                    stackOverflowClient.searchOnion(queries, language, exceptionType);
            log.debug("StackOverflow search done — {} results", results.size());
            return results;
        }, searchExecutor);
    }
    // Result carrier
    public SearchResults searchParallel(
            List<String> ghQueries,
            List<String> soQueries,
            String language,
            String exceptionType
    ) {
        long start = System.currentTimeMillis();

        CompletableFuture<List<GitHubIssue>> ghFuture =
                searchGitHubAsync(ghQueries);

        CompletableFuture<List<StackOverflowQuestion>> soFuture =
                searchStackOverflowAsync(soQueries, language, exceptionType);

        // Wait for both searches to complete
        CompletableFuture.allOf(ghFuture, soFuture).join();

        log.info("Parallel search finished in {}ms", System.currentTimeMillis() - start);

        return new SearchResults(ghFuture.join(), soFuture.join());
    }
}
