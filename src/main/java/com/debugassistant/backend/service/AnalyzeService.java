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
import java.util.Collections;
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

    public AnalyzeResponse analyze(AnalyzeRequest request) {
        log.info("Analyzing stack trace");

        // parse the error to get language and message
        ParsedError parsed = parserRegistry.parse(request.getStackTrace());
        log.debug("Parsed: type={}, message={}", parsed.exceptionType(), parsed.message());

        // build query and search on GitHub
        String query = parsed.exceptionType() + " " + parsed.message();
        List<GitHubIssue> issues = gitHubClient.searchIssues(query);

        // convert and rank the results
        List<SearchResult> results = issues.stream()
                .map(this::mapToSearchResult)
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed()) // best score first
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
        // handle null reactions
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