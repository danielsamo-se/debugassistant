package com.debugassistant.backend.dto.ml;

import java.util.List;
import java.util.Map;

/**
 * Response from ML service /analyze endpoint
 */
public record MlAnalyzeResponse(
        String analysis,
        List<MlSimilarError> similar_errors,
        boolean context_used
) {}
