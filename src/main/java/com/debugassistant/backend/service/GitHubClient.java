package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.github.GitHubIssue;
import com.debugassistant.backend.dto.github.GitHubSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Handles the connection to GitHub and searches for possible solutions.
 */
@Service
@Slf4j
public class GitHubClient {

    private final RestClient restClient;

    public GitHubClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<GitHubIssue> searchIssues(String query) {

        // filters for better results
        String smartQuery = query + " is:issue is:public stars:>10 sort:reactions-desc";

        log.info("GitHub search query: {}", smartQuery);

        try {
            GitHubSearchResponse response = restClient.get()
                    .uri(uri -> uri.path("/search/issues")
                            .queryParam("q", smartQuery)
                            .queryParam("per_page", 5)
                            .build())
                    .retrieve()

                    .onStatus(
                            status -> status.value() == 429,
                            (req, res) -> log.warn("GitHub rate limit reached")
                    )

                    .onStatus(
                            status -> status.value() == 404,
                            (req, res) -> log.warn("GitHub returned 404 for query")
                    )

                    .body(GitHubSearchResponse.class);

            // empty list to not crash
            if (response == null || response.items() == null) {
                return List.of();
            }

            return response.items();

        } catch (Exception e) {
            // fallback if GitHub is down or rate limit is hit
            log.error("GitHub request failed: {}", e.getMessage());
            return List.of();
        }
    }
}