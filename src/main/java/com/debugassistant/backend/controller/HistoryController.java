package com.debugassistant.backend.controller;

import com.debugassistant.backend.dto.history.HistoryResponse;
import com.debugassistant.backend.dto.history.SaveHistoryRequest;
import com.debugassistant.backend.entity.SearchHistory;
import com.debugassistant.backend.entity.User;
import com.debugassistant.backend.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints for user search history
 */
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Search History", description = "Endpoints for managing user search history")
public class HistoryController {

    private final HistoryService historyService;

    @PostMapping
    @Operation(summary = "Save history", description = "Saves search result for current user")
    public ResponseEntity<HistoryResponse> saveHistory(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SaveHistoryRequest request) {

        log.info("Saving history entry for user {}", user.getEmail());

        SearchHistory saved = historyService.saveSearch(
                user,
                request.stackTraceSnippet(),
                request.language(),
                request.exceptionType(),
                request.searchUrl()
        );

        return ResponseEntity.ok(HistoryResponse.from(saved));
    }

    @GetMapping
    @Operation(summary = "Get history", description = "Returns history list for current user")
    public ResponseEntity<List<HistoryResponse>> getHistory(@AuthenticationPrincipal User user) {
        List<HistoryResponse> history = historyService.getUserHistory(user.getId())
                .stream()
                .map(HistoryResponse::from)
                .toList();

        return ResponseEntity.ok(history);
    }
}