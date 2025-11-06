package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.AnalyzeRequest;
import com.debugassistant.backend.dto.AnalyzeResponse;
import com.debugassistant.backend.dto.AnalyzeResponse.SearchResult;
import com.debugassistant.backend.dto.github.GitHubIssue;
import com.debugassistant.backend.parser.ParsedError;
import com.debugassistant.backend.parser.ParserRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Connects the parser, GitHub client and ranking service to find solutions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyzeService {

    private final ParserRegistry parserRegistry;
    private final RankingService rankingService;
    private final GitHubClient gitHubClient;
    private final QueryBuilder queryBuilder; // <-- NEU

    public AnalyzeResponse analyze(AnalyzeRequest request) {
        log.info("Analyzing stack trace");

        // parse the error
        ParsedError parsed = parserRegistry.parse(request.getStackTrace());
        log.debug("Parsed: type={}, message={}", parsed.exceptionType(), parsed.message());

        // build a smart GitHub query
        String query = queryBuilder.buildSmartQuery(parsed);
        log.info("Generated GitHub query: {}", query);

        // search on GitHub
        List<GitHubIssue> issues = gitHubClient.searchIssues(query);

        // convert and rank results
        List<SearchResult> results = issues.stream()
                .map(this::mapToSearchResult)
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .toList();

        return AnalyzeResponse.builder()
                .language(parsed.language())
                .exceptionType(parsed.exceptionType())
                .message(parsed.message())
                .keywords(parsed.keywords())
                .rootCause(parsed.rootCause())
                .score(results.isEmpty() ? 0 : results.get(0).getScore().intValue())
                .results(results)
                .timestamp(Instant.now())
                .build();
    }

    private SearchResult mapToSearchResult(GitHubIssue issue) {
        double score = rankingService.calculateScore(issue);
        int reactions = (issue.reactions() != null && issue.reactions().totalCount() != null)
                ? issue.reactions().totalCount()
                : 0;

        return new SearchResult(
                "GitHub",
                issue.title(),
                issue.htmlUrl(),
                reactions,
                null,
                score
        );
    }
}