package com.debugassistant.backend.controller;

import com.debugassistant.backend.dto.history.HistoryResponse;
import com.debugassistant.backend.dto.history.SaveHistoryRequest;
import com.debugassistant.backend.entity.SearchHistory;
import com.debugassistant.backend.entity.User;
import com.debugassistant.backend.service.HistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @PostMapping
    public ResponseEntity<HistoryResponse> saveHistory(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SaveHistoryRequest request) {

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
    public ResponseEntity<List<HistoryResponse>> getHistory(@AuthenticationPrincipal User user) {
        List<HistoryResponse> history = historyService.getUserHistory(user.getId())
                .stream()
                .map(HistoryResponse::from)
                .toList();

        return ResponseEntity.ok(history);
    }
}