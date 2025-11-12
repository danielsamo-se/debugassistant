package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.github.GitHubIssue;
import com.debugassistant.backend.dto.github.GitHubSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
                    .body(GitHubSearchResponse.class);

            // empty list to not crash
            if (response == null || response.items() == null) {
                return List.of();
            }

            return response.items();

        } catch (Exception e) {
            //fallback if GitHub is down or rate limit is hiz
            log.error("GitHub request failed: {}", e.getMessage());
            return List.of();
        }
    }
}