package com.debugassistant.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request to ML service /analyze endpoint
 */
public record MlAnalyzeRequest(
        @JsonProperty("stack_trace")
        String stackTrace,

        @JsonProperty("use_retrieval")
        boolean useRetrieval
) {}
