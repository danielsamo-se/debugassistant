package com.debugassistant.backend.controller;

import com.debugassistant.backend.dto.AnalyzeRequest;
import com.debugassistant.backend.dto.github.GitHubIssue;
import com.debugassistant.backend.service.GitHubClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyzeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GitHubClient gitHubClient;

    @Test
    void analyzeReturnsStructuredResponse() throws Exception {
        GitHubIssue issue = new GitHubIssue(
                "Fix NPE",
                "http://example.com/1",
                "open",
                3,
                new GitHubIssue.Reactions(10),
                Instant.parse("2024-01-01T10:00:00Z"),
                "Some body text"
        );

        when(gitHubClient.searchIssues(anyString()))
                .thenReturn(List.of(issue));

        AnalyzeRequest request = new AnalyzeRequest("""
                java.lang.NullPointerException: boom
                    at com.example.Test.main(Test.java:10)
                """);

        mockMvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("java"))
                .andExpect(jsonPath("$.exceptionType").value("NullPointerException"))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results[0].title").value("Fix NPE"));
    }
}