package com.debugassistant.backend.service;

import com.debugassistant.backend.entity.SearchHistory;
import com.debugassistant.backend.entity.User;
import com.debugassistant.backend.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles storing and retrieving user search history
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryService {

    private final SearchHistoryRepository searchHistoryRepository;

    public SearchHistory saveSearch(User user, String stackTraceSnippet, String language,
                                    String exceptionType, String searchUrl) {
        // limit stack trace snippet to 500 characters
        SearchHistory history = SearchHistory.builder()
                .user(user)
                .stackTraceSnippet(stackTraceSnippet.length() > 500
                        ? stackTraceSnippet.substring(0, 500)
                        : stackTraceSnippet)
                .language(language)
                .exceptionType(exceptionType)
                .searchUrl(searchUrl)
                .searchedAt(LocalDateTime.now())
                .build();

        log.debug("Saving search history for user: {}", user.getEmail());
        return searchHistoryRepository.save(history);
    }

    public List<SearchHistory> getUserHistory(Long userId) {
        return searchHistoryRepository.findByUserIdOrderBySearchedAtDesc(userId);
    }
}