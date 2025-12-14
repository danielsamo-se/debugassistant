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
            key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(" +
                    "#request.stackTrace().trim().replace('\\r\\n','\\n')" +
                    ".getBytes(T(java.nio.charset.StandardCharsets).UTF_8))"
    )
    public AnalyzeResponse analyze(AnalyzeRequest request) {
        log.info("Nothing found in cache, analyzing stack trace");
        ParsedError parsed = parserRegistry.parse(request.stackTrace());
        log.info("Parsed {} error: {}", parsed.language(), parsed.exceptionType());

        String query = queryBuilder.buildSmartQuery(parsed, request.stackTrace());

        // search both sources in parallel (could be async later)
        List<GitHubIssue> githubIssues = gitHubClient.searchIssues(query);
        List<StackOverflowQuestion> soQuestions = stackOverflowClient.search(query, parsed.language());

        log.debug("Found {} GitHub issues, {} Stack Overflow questions",
                githubIssues.size(), soQuestions.size());

        // merge and rank results
        List<SearchResult> results = new ArrayList<>();

        for (GitHubIssue issue : githubIssues) {
            results.add(toSearchResult(issue, parsed.keywords()));
        }

        for (StackOverflowQuestion question : soQuestions) {
            results.add(toSearchResult(question, parsed.keywords()));
        }

        // sort descending by score
        results.sort(Comparator.comparingDouble(SearchResult::getScore).reversed());

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