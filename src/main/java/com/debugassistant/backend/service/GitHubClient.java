package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.github.GitHubIssue;
import com.debugassistant.backend.dto.github.GitHubSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Searches GitHub Issues for possible solutions
 */
@Service
@Slf4j
public class GitHubClient {

    private final RestClient restClient;

    public GitHubClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<GitHubIssue> searchOnion(List<String> queries) {
        if (queries == null || queries.isEmpty()) {
            return List.of(); // no queries
        }

        Map<String, GitHubIssue> dedup = new LinkedHashMap<>(); // stable order + dedup

        for (String q : queries) {
            List<GitHubIssue> batch = searchIssues(q); // layered recall
            for (GitHubIssue issue : batch) {
                if (issue != null && issue.htmlUrl() != null) {
                    dedup.putIfAbsent(issue.htmlUrl(), issue);  // url identity
                }
            }
        }

        return new ArrayList<>(dedup.values());
    }

    public List<GitHubIssue> searchIssues(String query) {
        if (query == null || query.isBlank()) {
            return List.of(); // invalid query
        }

        String fullQuery = normalize(query); // collapse whitespace

        if (fullQuery.length() > 250) {
            fullQuery = fullQuery.substring(0, 250).trim(); // query size cap
        }

        if (!containsInQualifier(fullQuery)) {
            fullQuery = normalize(fullQuery + " in:title,body"); // scope to text fields
        }

        if (!fullQuery.contains("is:issue") && !fullQuery.contains("is:pull-request")) {
            fullQuery = normalize(fullQuery + " is:issue"); // avoid PR noise
        }

        final String q = fullQuery;

        log.info("GitHub query: {}", q);

        try {
            GitHubSearchResponse response = restClient.get()
                    .uri(uri -> uri.path("/search/issues")
                            .queryParam("q", q)
                            .queryParam("per_page", 10) // small payload
                            .queryParam("sort", "reactions") // social proof
                            .queryParam("order", "desc") // best first
                            .build())
                    .retrieve()
                    .body(GitHubSearchResponse.class);

            if (response == null || response.items() == null) {
                log.info("Found 0 GitHub issues");
                return List.of();
            }

            log.info("Found {} GitHub issues", response.items().size());
            return response.items();

        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.TooManyRequests e) {
            log.warn("GitHub rate limit reached");
            return List.of(); // graceful degrade

        } catch (HttpServerErrorException e) {
            log.warn("GitHub API error");
            return List.of(); // upstream failure

        } catch (Exception e) {
            log.error("GitHub request failed", e);
            return List.of(); // safety net
        }
    }

    private boolean containsInQualifier(String q) {
        String s = q == null ? "" : q.toLowerCase();
        return s.contains(" in:title") || s.contains(" in:body") || s.contains(" in:comments"); // qualifier check
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", " ").trim(); // normalize whitespace
    }
}

