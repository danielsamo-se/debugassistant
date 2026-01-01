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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Main service that connects parsing, searching and ranking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyzeService {

    private static final double GITHUB_SCORE_THRESHOLD = 0.40;
    private static final double STACKOVERFLOW_SCORE_THRESHOLD = 0.30;
    private static final double STACKOVERFLOW_ANSWERED_BOOST = 1.15;

    private final ParserRegistry parserRegistry;
    private final QueryBuilder queryBuilder;
    private final GitHubClient gitHubClient;
    private final StackOverflowClient stackOverflowClient;
    private final RankingService rankingService;

    @Cacheable(
            value = "analyses",
            // stable cache key: trim + normalize CRLF
            key = "T(org.springframework.util.DigestUtils).md5DigestAsHex((" +
                    "#request.stackTrace().trim().replace(\"\\r\\n\", \"\\n\")" +
                    ").getBytes(T(java.nio.charset.StandardCharsets).UTF_8))"
    )
    public AnalyzeResponse analyze(AnalyzeRequest request) {
        log.info("Nothing found in cache, analyzing stack trace");

        ParsedError parsed = parserRegistry.parse(request.stackTrace());
        log.info("Parsed {} error: {}", parsed.language(), parsed.exceptionType());

        List<String> ghQueries = queryBuilder.buildGitHubQueries(parsed, request.stackTrace()); // fallback queries
        List<GitHubIssue> githubIssues = gitHubClient.searchOnion(ghQueries); // broaden recall

        List<String> soQueries = queryBuilder.buildStackOverflowQueries(parsed, request.stackTrace()); // SO-specific
        List<StackOverflowQuestion> soQuestions = stackOverflowClient.searchOnion(
                soQueries, parsed.language(), parsed.exceptionType()
        ); // reduce ambiguity

        log.info("Found {} GitHub issues, {} Stack Overflow questions",
                githubIssues.size(), soQuestions.size());

        List<SearchResult> results = new ArrayList<>();

        Set<String> gitHubKeywords = enrichGitHubKeywords(parsed); // improve matching on GH metadata

        for (GitHubIssue issue : githubIssues) {
            SearchResult result = toSearchResult(issue, gitHubKeywords);
            if (result.getScore() >= GITHUB_SCORE_THRESHOLD) {
                results.add(result);
            }
        }

        for (StackOverflowQuestion question : soQuestions) {
            SearchResult result = toSearchResult(question, parsed.keywords());
            if (Boolean.TRUE.equals(result.isAnswered())) {
                result = boostAnsweredStackOverflow(result); // prefer accepted/solved
            }
            if (result.getScore() >= STACKOVERFLOW_SCORE_THRESHOLD) {
                results.add(result);
            }
        }

        results.sort(Comparator.comparingDouble(SearchResult::getScore).reversed()); // rank descending

        if (results.size() > 15) {
            results = new ArrayList<>(results.subList(0, 15)); // cap payload
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

    private SearchResult boostAnsweredStackOverflow(SearchResult result) {
        return new SearchResult(
                result.source(),
                result.title(),
                result.url(),
                result.reactions(),
                result.snippet(),
                result.getScore() * STACKOVERFLOW_ANSWERED_BOOST,
                result.answerCount(),
                result.isAnswered()
        );
    }

    private Set<String> enrichGitHubKeywords(ParsedError parsed) {
        Set<String> out = new HashSet<>();

        if (parsed.keywords() != null) {
            out.addAll(parsed.keywords());
        }

        if (parsed.exceptionType() != null && !parsed.exceptionType().isBlank()) {
            out.add(simpleName(parsed.exceptionType()));
            out.add(parsed.exceptionType());
        }

        if (parsed.rootCause() != null && !parsed.rootCause().isBlank()) {
            String rc = parsed.rootCause().split(":")[0].trim();  // drop message tail
            if (!rc.isBlank()) {
                out.add(simpleName(rc));
                out.add(rc);
            }
        }

        return out;
    }

    private String simpleName(String value) {
        if (value == null) return "";
        String v = value.trim();
        int idx = v.lastIndexOf('.');
        return idx >= 0 ? v.substring(idx + 1) : v; // class name only
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