package com.debugassistant.backend.dto.history;

import com.debugassistant.backend.entity.SearchHistory;

import java.time.LocalDateTime;

/**
 * Response DTO for returning saved search history entries
 */
public record HistoryResponse(
        Long id,
        String stackTraceSnippet,
        String language,
        String exceptionType,
        String searchUrl,
        LocalDateTime searchedAt
) {
    public static HistoryResponse from(SearchHistory history) {
        return new HistoryResponse(
                history.getId(),
                history.getStackTraceSnippet(),
                history.getLanguage(),
                history.getExceptionType(),
                history.getSearchUrl(),
                history.getSearchedAt()
        );
    }
}