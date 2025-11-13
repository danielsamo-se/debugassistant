package com.debugassistant.backend.dto.github;

import java.util.List;

/**
 * Wraps the raw response from the GitHub Search API.
 */
public record GitHubSearchResponse(
        List<GitHubIssue> items
) {}
