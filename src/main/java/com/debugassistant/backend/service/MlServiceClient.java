package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.ml.MlAnalyzeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class MlServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String mlServiceUrl;

    public MlServiceClient(@Value("${ml.service.url:http://localhost:8000}") String mlServiceUrl) {
        this.mlServiceUrl = mlServiceUrl;
    }

    public Optional<MlAnalyzeResponse> analyze(String stackTrace) {
        return analyze(stackTrace, true);
    }

    public Optional<MlAnalyzeResponse> analyze(String stackTrace, boolean useRetrieval) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "stack_trace", stackTrace,
                    "use_retrieval", useRetrieval
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<MlAnalyzeResponse> response = restTemplate.postForEntity(
                    mlServiceUrl + "/analyze",
                    request,
                    MlAnalyzeResponse.class
            );

            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("ML service call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public boolean isHealthy() {
        try {
            restTemplate.getForEntity(mlServiceUrl + "/health", String.class);
            return true;
        } catch (Exception e) {
            log.warn("ML service health check failed: {}", e.getMessage());
            return false;
        }
    }
}