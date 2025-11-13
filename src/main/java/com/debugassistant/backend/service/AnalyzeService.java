package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.AnalyzeRequest;
import com.debugassistant.backend.dto.AnalyzeResponse;
import com.debugassistant.backend.dto.SearchResult;
import com.debugassistant.backend.dto.github.GitHubIssue;
import com.debugassistant.backend.parser.ParsedError;
import com.debugassistant.backend.parser.ParserRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Main service that connects parsing, searching and ranking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyzeService {

    private final ParserRegistry parserRegistry;
    private final QueryBuilder queryBuilder;
    private final GitHubClient gitHubClient;
    private final RankingService rankingService;

    public AnalyzeResponse analyze(AnalyzeRequest request) {
        ParsedError parsed = parserRegistry.parse(request.stackTrace());
        log.info("Parsed {} error: {}", parsed.language(), parsed.exceptionType());

        String query = queryBuilder.buildSmartQuery(parsed);

        List<GitHubIssue> issues = gitHubClient.searchIssues(query);
        log.debug("Found {} GitHub issues", issues.size());

        List<SearchResult> results = issues.stream()
                .map(issue -> toSearchResult(issue, parsed.keywords()))
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .toList();

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
        double score = rankingService.calculateScore(issue, keywords);

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
                score
        );
    }
}