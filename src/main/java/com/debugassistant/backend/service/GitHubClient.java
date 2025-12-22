package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.github.GitHubIssue;
import com.debugassistant.backend.dto.github.GitHubSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
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
        if (query == null || query.isBlank()) {
            return List.of();
        }

        if (query.length() > 250) {
            query = query.substring(0, 250).trim();
        }

        String fullQuery = query + " is:issue sort:reactions-desc";
        log.debug("GitHub query: {}", fullQuery);

        try {
            GitHubSearchResponse response = restClient.get()
                    .uri(uri -> uri.path("/search/issues")
                            .queryParam("q", fullQuery)
                            .queryParam("per_page", 10)
                            .build())
                    .retrieve()
                    .body(GitHubSearchResponse.class);

            if (response == null || response.items() == null) {
                log.debug("No results from GitHub");
                return List.of();
            }

            log.debug("Found {} issues", response.items().size());
            return response.items();

        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.TooManyRequests e) {
            log.warn("GitHub rate limit reached");
            return List.of();

        } catch (HttpServerErrorException e) {
            log.warn("GitHub API error");
            return List.of();

        } catch (Exception e) {
            log.error("GitHub request failed", e);
            return List.of();
        }
    }
}