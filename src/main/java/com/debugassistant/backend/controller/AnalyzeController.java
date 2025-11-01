package com.debugassistant.backend.controller;

import com.debugassistant.backend.dto.AnalyzeRequest;
import com.debugassistant.backend.dto.AnalyzeResponse;
import com.debugassistant.backend.service.AnalyzeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analyze")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analyze", description = "Stack trace analysis endpoints")
public class AnalyzeController {

    private final AnalyzeService analyzeService;

    @PostMapping
    @Operation(summary = "Analyze stack trace",
            description = "Parses a Java stack trace and returns exception details")
    public ResponseEntity<AnalyzeResponse> analyze(
            @Valid @RequestBody AnalyzeRequest request) {

        log.info("Received analyze request");
        AnalyzeResponse response = analyzeService.analyze(request);
        return ResponseEntity.ok(response);
    }
}