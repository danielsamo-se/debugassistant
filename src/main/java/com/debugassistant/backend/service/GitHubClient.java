package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.github.GitHubIssue;
import com.debugassistant.backend.dto.github.GitHubSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

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

    public List<GitHubIssue> searchIssues(String query) {
        String fullQuery = query + " is:issue sort:reactions-desc";

        log.debug("GitHub query: {}", fullQuery);

        try {
            GitHubSearchResponse response = restClient.get()
                    .uri(uri -> uri.path("/search/issues")
                            .queryParam("q", fullQuery)
                            .queryParam("per_page", 10)
                            .build())
                    .retrieve()
                    .onStatus(
                            status -> status.value() == 429,
                            (req, res) -> log.warn("GitHub rate limit reached")
                    )
                    .onStatus(
                            status -> status.value() == 404,
                            (req, res) -> log.warn("GitHub returned 404")
                    )
                    .body(GitHubSearchResponse.class);

            if (response == null || response.items() == null) {
                log.debug("No results from GitHub");
                return List.of();
            }

            log.debug("Found {} issues", response.items().size());
            return response.items();

        } catch (Exception e) {
            log.error("GitHub request failed: {}", e.getMessage());
            return List.of();
        }
    }
}