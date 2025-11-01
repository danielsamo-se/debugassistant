package com.debugassistant.backend.ranking;

import com.debugassistant.backend.parser.ParsedError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RankingService {

    public int calculateDummyScore(ParsedError parsed) {
        int score = parsed.message() != null ? parsed.message().length() : 0;
        log.debug("Calculated dummy score: {}", score);
        return score;
    }
}

