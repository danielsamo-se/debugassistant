package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.stackoverflow.StackOverflowQuestion;
import com.debugassistant.backend.dto.stackoverflow.StackOverflowResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

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

    public List<StackOverflowQuestion> search(String query, String language) {
        String tagged = mapLanguageToTag(language);

        log.debug("Stack Overflow query: {} [tag: {}]", query, tagged);

        try {
            StackOverflowResponse response = restClient.get()
                    .uri(uri -> uri.path("/search/advanced")
                            .queryParam("order", "desc")
                            .queryParam("sort", "votes")
                            .queryParam("accepted", "true")
                            .queryParam("site", "stackoverflow")
                            .queryParam("q", query)
                            .queryParam("tagged", tagged)
                            .queryParam("pagesize", "10")
                            .build())
                    .retrieve()
                    .body(StackOverflowResponse.class);

            if (response == null || response.items() == null) {
                log.debug("No results from Stack Overflow");
                return List.of();
            }

            log.debug("Found {} Stack Overflow questions (quota remaining: {})",
                    response.items().size(), response.quotaRemaining());

            return response.items();

        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.TooManyRequests e) {
            log.warn("StackOverflow Rate Limit hit");
            return List.of();

        } catch (HttpServerErrorException e) {
            log.warn("StackOverflow API error ");
            return List.of();

        } catch (Exception e) {
            // other failure
            log.error("Stack Overflow request failed: {}", e.getMessage());
            return List.of();
        }
    }

    private String mapLanguageToTag(String language) {
        if (language == null) return "";
        return switch (language.toLowerCase()) {
            case "java" -> "java";
            case "python" -> "python";
            default -> "";
        };
    }
}