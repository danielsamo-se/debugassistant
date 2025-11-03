package com.debugassistant.backend.dto.github;

import java.util.List;

/**
 * Wraps the raw response from the GitHub Search API.
 * We are only interested in the list of found items (issues), ignoring metadata like total_count.
 */
public record GitHubSearchResponse(
        List<GitHubIssue> items
) {}
