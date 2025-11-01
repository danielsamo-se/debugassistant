package com.debugassistant.backend.ranking;

import com.debugassistant.backend.parser.ParsedError;
import org.springframework.stereotype.Service;

@Service
public class RankingService {

    // dummy score for now
    public int calculateDummyScore(ParsedError parsed) {
        return parsed.message().length();
    }
}
