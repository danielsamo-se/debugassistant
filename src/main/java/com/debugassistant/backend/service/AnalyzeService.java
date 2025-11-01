package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.AnalyzeRequest;
import com.debugassistant.backend.dto.AnalyzeResponse;
import com.debugassistant.backend.parser.ParserRegistry;
import com.debugassistant.backend.parser.ParsedError;
import com.debugassistant.backend.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyzeService {

    private final ParserRegistry parserRegistry;
    private final RankingService rankingService;

    public AnalyzeResponse analyze(AnalyzeRequest request) {
        ParsedError parsed = parserRegistry.parse(request.getStackTrace());
        int score = rankingService.calculateDummyScore(parsed);

        return AnalyzeResponse.builder()
                .language(parsed.language())
                .exceptionType(parsed.exceptionType())
                .message(parsed.message())
                .score(score)
                .build();
    }
}