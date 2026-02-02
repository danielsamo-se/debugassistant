package com.debugassistant.backend.dto.ml;

import java.util.Map;

/**
 * Similar error returned from ML service
 */
public record MlSimilarError(
        double score,
        Map<String, Object> metadata
) {}