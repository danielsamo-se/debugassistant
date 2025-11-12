package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.github.GitHubIssue;
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
    void returnsParsedIssuesOnSuccess() {
        String json = """
            {
              "items": [
                {
                  "title": "Fix NullPointer",
                  "body": "Something failed",
                  "html_url": "http://example.com/1",
                  "state": "open",
                  "comments": 3,
                  "reactions": { "total_count": 10 },
                  "created_at": "2024-01-01T10:00:00Z"
                }
              ]
            }
        """;

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(json)
                .addHeader("Content-Type", "application/json"));

        List<GitHubIssue> issues = gitHubClient.searchIssues("NullPointerException");

        assertThat(issues).hasSize(1);
        GitHubIssue issue = issues.getFirst();

        assertThat(issue.title()).isEqualTo("Fix NullPointer");
        assertThat(issue.body()).isEqualTo("Something failed");
        assertThat(issue.htmlUrl()).isEqualTo("http://example.com/1");
        assertThat(issue.reactions().totalCount()).isEqualTo(10);
    }

    @Test
    void returnsEmptyListWhenServerErrors() {
        mockServer.enqueue(new MockResponse().setResponseCode(500));

        List<GitHubIssue> issues = gitHubClient.searchIssues("anything");

        assertThat(issues).isEmpty();
    }
}