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
import java.util.Set;

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
        // parse
        ParsedError parsed = parserRegistry.parse(request.getStackTrace());

        // build query & search GitHub
        String query = queryBuilder.build(parsed.exceptionType(), parsed.message(), parsed.keywords());
        List<GitHubIssue> issues = gitHubClient.searchIssues(query);

        // map results + ranking
        List<SearchResult> results = issues.stream()
                .map(issue -> mapToSearchResult(issue, parsed.keywords()))
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

    private SearchResult mapToSearchResult(GitHubIssue issue, Set<String> keywords) {
        double score = rankingService.calculateScore(issue, keywords);

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