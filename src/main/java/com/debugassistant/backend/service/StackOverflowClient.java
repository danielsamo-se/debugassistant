package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.stackoverflow.StackOverflowQuestion;
import com.debugassistant.backend.dto.stackoverflow.StackOverflowResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
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

        try {
            UriComponentsBuilder b = UriComponentsBuilder
                    .fromPath("/search/advanced")
                    .queryParam("site", "stackoverflow")
                    .queryParam("order", "desc")
                    .queryParam("sort", "relevance")
                    .queryParam("pagesize", 30)
                    .queryParam("accepted", "true")
                    .queryParam("answers", 1) // only questions with answers
                    .queryParam("q", query);

            // add tag filter only if supported
            if (!tagged.isBlank()) {
                b.queryParam("tagged", tagged);
            }

            // encode to avoid invalid URI
            String uri = b.encode(StandardCharsets.UTF_8).build().toUriString();
            log.debug("StackOverflow request URI: {}", uri);

            StackOverflowResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(StackOverflowResponse.class);

            // treat empty payload as no results
            if (response == null || response.items() == null) {
                log.debug("No results from Stack Overflow");
                return List.of();
            }

            log.debug("Found {} Stack Overflow questions (quota remaining: {})",
                    response.items().size(), response.quotaRemaining());

            return response.items();

            // rate limit so return empty
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.TooManyRequests e) {
            log.warn("StackOverflow rate limit/forbidden: {}", e.getStatusCode());
            return List.of();

        } catch (HttpClientErrorException e) {
            log.warn("StackOverflow client error: {} {}", e.getStatusCode(), e.getMessage());
            return List.of();

        } catch (HttpServerErrorException e) {
            log.warn("StackOverflow server error: {} {}", e.getStatusCode(), e.getMessage());
            return List.of();

        } catch (Exception e) {
            log.error("StackOverflow request failed: {}", e.toString());
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