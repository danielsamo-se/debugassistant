package com.debugassistant.backend.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Represents a GitHub issue from the Search API
 */
public record GitHubIssue(
        String title,

        @JsonProperty("html_url")
        String htmlUrl,

        String state,
        Integer comments,
        Reactions reactions,

        @JsonProperty("created_at")
        Instant createdAt,

        String body
) {
    public record Reactions(
            @JsonProperty("total_count")
            Integer totalCount
    ) {}
}