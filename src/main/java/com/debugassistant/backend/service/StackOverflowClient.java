package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.stackoverflow.StackOverflowQuestion;
import com.debugassistant.backend.dto.stackoverflow.StackOverflowResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Searches Stack Overflow for possible solutions
 */
@Service
@Slf4j
public class StackOverflowClient {

    private final RestClient restClient;

    public StackOverflowClient(@Qualifier("stackOverflowRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    private String mapLanguageToTag(String language) {
        if (language == null) return "";
        return switch (language.toLowerCase()) {
            case "java" -> "java";
            case "python" -> "python";
            default -> "";
        };
    }

    public List<StackOverflowQuestion> searchOnion(List<String> queries, String language, String exceptionType) {
        if (queries == null || queries.isEmpty()) return List.of();

        String tagged = mapLanguageToTag(language); // narrow by language tag
        List<StackOverflowQuestion> collected = new ArrayList<>();
        int MIN_RESULTS = 5; // early stop threshold

        // strict -> broad
        for (String q : queries) {
            if (q == null || q.isBlank()) continue; // skip blanks

            var res = searchAdvanced(q, tagged, true, false); // require answers
            if (!res.isEmpty()) {
                collected.addAll(res);
                if (collected.size() >= MIN_RESULTS) return collected; // stop early
            }

            res = searchAdvanced(q, tagged, false, false); // relax constraints
            if (!res.isEmpty()) {
                collected.addAll(res);
                if (collected.size() >= MIN_RESULTS) return collected; // stop early
            }
        }
        return collected;
    }

    private List<StackOverflowQuestion> searchAdvanced(String q, String tagged, boolean requireAnswers, boolean requireAccepted) {
        try {
            UriComponentsBuilder b = UriComponentsBuilder
                    .fromPath("/search/advanced")
                    .queryParam("site", "stackoverflow")
                    .queryParam("order", "desc")
                    .queryParam("sort", "relevance")
                    .queryParam("pagesize", 30);

            if (requireAnswers) b.queryParam("answers", 1);          // filter zero-answer
            if (requireAccepted) b.queryParam("accepted", "true");   // stricter filter
            if (tagged != null && !tagged.isBlank()) b.queryParam("tagged", tagged); // language scope
            if (q != null && !q.isBlank()) b.queryParam("q", q);     // query text

            String uri = b.encode(StandardCharsets.UTF_8).build().toUriString(); // safe encoding
            log.info("StackOverflow request URI: {}", uri);

            StackOverflowResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(StackOverflowResponse.class);

            if (response == null || response.items() == null) return List.of();
            return response.items();

        } catch (Exception e) {
            log.debug("StackOverflow request failed: {}", e.toString());
            return List.of();
        }
    }
}
