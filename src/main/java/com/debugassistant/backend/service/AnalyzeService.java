package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.AnalyzeRequest;
import com.debugassistant.backend.dto.AnalyzeResponse;
import com.debugassistant.backend.dto.SearchResult;
import com.debugassistant.backend.dto.github.GitHubIssue;
import com.debugassistant.backend.dto.stackoverflow.StackOverflowQuestion;
import com.debugassistant.backend.parser.ParsedError;
import com.debugassistant.backend.parser.ParserRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Main service that connects parsing, searching and ranking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyzeService {

    private final ParserRegistry parserRegistry;
    private final QueryBuilder queryBuilder;
    private final GitHubClient gitHubClient;
    private final StackOverflowClient stackOverflowClient;
    private final RankingService rankingService;

    @Cacheable(
            value = "analyses",
            // Cache key = normalized stacktrace (stable across whitespace/line endings)
            key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(" +
                    "#request.stackTrace().trim().replace('\\r\\n','\\n')" +
                    ".getBytes(T(java.nio.charset.StandardCharsets).UTF_8))"
    )
    public AnalyzeResponse analyze(AnalyzeRequest request) {
        log.info("Nothing found in cache, analyzing stack trace");

        // Extract language + keywords
        ParsedError parsed = parserRegistry.parse(request.stackTrace());
        log.info("Parsed {} error: {}", parsed.language(), parsed.exceptionType());

        // GitHub search query
        String githubQuery = queryBuilder.buildGitHubQuery(parsed, request.stackTrace());
        List<GitHubIssue> githubIssues = gitHubClient.searchIssues(githubQuery);

        // Multiple SO queries for broader matches
        List<String> soQueries = queryBuilder.buildStackOverflowQueries(parsed, request.stackTrace());
        List<StackOverflowQuestion> soQuestions = stackOverflowClient.searchOnion(
                soQueries, parsed.language(), parsed.exceptionType()
        );

        log.debug("Found {} GitHub issues, {} Stack Overflow questions",
                githubIssues.size(), soQuestions.size());

        List<SearchResult> results = new ArrayList<>();

        for (GitHubIssue issue : githubIssues) {
            results.add(toSearchResult(issue, parsed.keywords()));
        }

        for (StackOverflowQuestion question : soQuestions) {
            results.add(toSearchResult(question, parsed.keywords()));
        }

        // Drop bad matches
        results.removeIf(r -> r.getScore() < 0);

        // Best score first
        results.sort(Comparator.comparingDouble(SearchResult::getScore).reversed());

        // Limit response size
        if (results.size() > 15) {
            results = new ArrayList<>(results.subList(0, 15));
        }

        return AnalyzeResponse.builder()
                .language(parsed.language())
                .exceptionType(parsed.exceptionType())
                .message(parsed.message())
                .keywords(parsed.keywords())
                .rootCause(parsed.rootCause())
                .results(results)
                .build();
    }

    private SearchResult toSearchResult(GitHubIssue issue, Set<String> keywords) {
        double score = rankingService.calculateGitHubScore(issue, keywords);

        // Missing reactions can be null
        int reactions = 0;
        if (issue.reactions() != null && issue.reactions().totalCount() != null) {
            reactions = issue.reactions().totalCount();
        }

        return new SearchResult(
                "github",
                issue.title(),
                issue.htmlUrl(),
                reactions,
                null,
                score,
                null,
                null
        );
    }

    private SearchResult toSearchResult(StackOverflowQuestion question, Set<String> keywords) {
        double score = rankingService.calculateStackOverflowScore(question, keywords);

        return new SearchResult(
                "stackoverflow",
                question.title(),
                question.link(),
                question.score(),
                null,
                score,
                question.answerCount(),
                question.isAnswered()
        );
    }
}