package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.github.GitHubIssue;
import com.debugassistant.backend.dto.github.GitHubSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

/**
 * Handles the connection to GitHub and searches for possible solutions.
 */
@Service
@Slf4j
public class GitHubClient {

    private final RestClient restClient;

    public GitHubClient(@Value("${github.token}") String token) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .build();
    }

    public List<GitHubIssue> searchIssues(String query) {
        // filter for quality: only popular repos (stars > 10) and sort by helpfulness
        String smartQuery = query + " is:issue is:public stars:>10 sort:reactions-desc";

        log.info("Searching GitHub with query: {}", smartQuery);

        try {
            GitHubSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/issues")
                            .queryParam("q", smartQuery)
                            .queryParam("per_page", 5)
                            .build())
                    .retrieve()
                    .body(GitHubSearchResponse.class);

            if (response == null || response.items() == null) {
                return Collections.emptyList();
            }

            return response.items();

        } catch (Exception e) {
            log.error("GitHub API call failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}