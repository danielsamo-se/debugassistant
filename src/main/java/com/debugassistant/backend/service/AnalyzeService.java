package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.AnalyzeRequest;
import com.debugassistant.backend.dto.AnalyzeResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyzeService {

    private final ErrorParser errorParser;

    public AnalyzeResponse analyze(AnalyzeRequest request) {

        // V1 Parser
        ParsedError parsed = errorParser.parse(request.getStackTrace());

        // Dummy Score (Woche 1)
        int score = parsed.message().length();

        return AnalyzeResponse.builder()
                .language(parsed.language())
                .exceptionType(parsed.exceptionType())
                .message(parsed.message())
                .score(score)
                .build();
    }
}