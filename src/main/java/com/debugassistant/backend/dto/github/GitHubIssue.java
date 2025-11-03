package com.debugassistant.backend.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubIssue(
        String title,

        @JsonProperty("html_url")
        String htmlUrl,

        String state,

        // can be null
        Integer comments,

        Reactions reactions
) {

    public record Reactions(
            @JsonProperty("total_count")
            Integer totalCount
    ) {}
}