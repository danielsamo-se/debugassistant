package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.AnalyzeRequest;
import com.debugassistant.backend.dto.AnalyzeResponse;
import com.debugassistant.backend.parser.ParserRegistry;
import com.debugassistant.backend.parser.ParsedError;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyzeService {

    private final ParserRegistry parserRegistry;

    public AnalyzeResponse analyze(AnalyzeRequest request) {

        ParsedError parsed = parserRegistry.parse(request.getStackTrace());

        // dummy score
        int score = parsed.message().length();

        return AnalyzeResponse.builder()
                .language(parsed.language())
                .exceptionType(parsed.exceptionType())
                .message(parsed.message())
                .score(score)
                .build();
    }
}