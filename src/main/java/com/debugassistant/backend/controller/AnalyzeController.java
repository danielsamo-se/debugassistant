package com.debugassistant.backend.controller;

import com.debugassistant.backend.dto.AnalyzeRequest;
import com.debugassistant.backend.dto.AnalyzeResponse;
import com.debugassistant.backend.entity.User;
import com.debugassistant.backend.service.AnalyzeService;
import com.debugassistant.backend.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint for stack trace analysis
 */
@RestController
@RequestMapping("/api/analyze")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analyze", description = "Stack trace analysis endpoints")
public class AnalyzeController {

    private final AnalyzeService analyzeService;
    private final HistoryService historyService;

    @PostMapping
    @Operation(summary = "Analyze stack trace", description = "Parses stack trace and returns solutions")
    public ResponseEntity<AnalyzeResponse> analyze(@Valid @RequestBody AnalyzeRequest request,
                                                   @AuthenticationPrincipal User user) {
        log.info("Received analyze request ({} chars)", request.stackTrace() == null ? 0 : request.stackTrace().length());

        AnalyzeResponse response = analyzeService.analyze(request);

        if (user != null) {
            String bestUrl = (response.results() == null || response.results().isEmpty())
                    ? ""
                    : response.results().getFirst().url();

            historyService.saveSearch(
                    user,
                    request.stackTrace(),
                    response.language(),
                    response.exceptionType(),
                    bestUrl
            );
        }

        return ResponseEntity.ok(response);
    }
}