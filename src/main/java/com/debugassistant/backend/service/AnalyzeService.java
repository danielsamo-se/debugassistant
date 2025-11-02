package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.AnalyzeRequest;
import com.debugassistant.backend.dto.AnalyzeResponse;
import com.debugassistant.backend.parser.ParsedError;
import com.debugassistant.backend.parser.ParserRegistry;
import com.debugassistant.backend.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyzeService {

    private final ParserRegistry parserRegistry;
    private final RankingService rankingService;

    public AnalyzeResponse analyze(AnalyzeRequest request) {
        log.info("Analyzing stack trace, length: {}", request.getStackTrace().length());

        ParsedError parsed = parserRegistry.parse(request.getStackTrace());

        log.debug("Parsed: type={}, message={}",
                parsed.exceptionType(), parsed.message());

        int score = rankingService.calculateDummyScore(parsed);

        return AnalyzeResponse.builder()
                .language(parsed.language())
                .exceptionType(parsed.exceptionType())
                .message(parsed.message())
                .keywords(parsed.keywords())
                .rootCause(parsed.rootCause())
                .score(score)
                .results(List.of())
                .timestamp(Instant.now())
                .build();
    }
}