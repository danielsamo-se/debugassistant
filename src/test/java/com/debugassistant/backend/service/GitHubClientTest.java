package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.github.GitHubIssue;
import com.debugassistant.backend.dto.github.GitHubSearchResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubClientTest {

    private MockWebServer mockServer;
    private GitHubClient gitHubClient;

    @BeforeEach
    void setup() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();
        RestClient restClient = RestClient.builder()
                .baseUrl(mockServer.url("/").toString())
                .build();
        gitHubClient = new GitHubClient(restClient);
    }

    @AfterEach
    void teardown() throws Exception {
        mockServer.shutdown();
    }

    @Test
    void happyPath() {
        enqueueJson("""
        {
          "items": [
            { "title": "Issue A", "body": "Body A", "html_url": "u", "state": "open", "reactions": {"total_count": 1} }
          ]
        }
        """);

        List<GitHubIssue> issues = gitHubClient.searchIssues("error");
        assertThat(issues).hasSize(1);
    }

    @Test
    void trimsQuery() throws Exception {
        enqueueJson("{\"items\": []}");
        gitHubClient.searchIssues("x".repeat(300));
        String q = mockServer.takeRequest().getRequestUrl().queryParameter("q");

        assertThat(q.length()).isLessThanOrEqualTo(300);
    }

    @Test
    void returnsEmptyWhenItemsNull() {
        enqueueJson("{ \"items\": null }");
        assertThat(gitHubClient.searchIssues("x")).isEmpty();
    }

    @Test
    void returnsEmptyOn403() {
        mockServer.enqueue(new MockResponse().setResponseCode(403));
        assertThat(gitHubClient.searchIssues("x")).isEmpty();
    }

    @Test
    void returnsEmptyOnBrokenJson() {
        mockServer.enqueue(new MockResponse().setBody("INVALID JSON"));
        assertThat(gitHubClient.searchIssues("x")).isEmpty();
    }

    private void enqueueJson(String json) {
        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(json));
    }
}