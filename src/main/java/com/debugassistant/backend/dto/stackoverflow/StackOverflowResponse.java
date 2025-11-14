package com.debugassistant.backend.dto.stackoverflow;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Wraps the response from Stack Overflow Search API
 */
public record StackOverflowResponse(
        List<StackOverflowQuestion> items,

        @JsonProperty("has_more")
        boolean hasMore,

        @JsonProperty("quota_remaining")
        int quotaRemaining
) {}