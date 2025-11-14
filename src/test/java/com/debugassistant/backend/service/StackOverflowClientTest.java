package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.stackoverflow.StackOverflowQuestion;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StackOverflowClientTest {

    private MockWebServer mockServer;
    private StackOverflowClient client;

    @BeforeEach
    void setup() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();

        RestClient restClient = RestClient.builder()
                .baseUrl(mockServer.url("/").toString())
                .build();

        client = new StackOverflowClient(restClient);
    }

    @AfterEach
    void teardown() throws Exception {
        mockServer.shutdown();
    }

    @Test
    void returnsParsedQuestionsOnSuccess() {
        String json = """
            {
              "items": [
                {
                  "question_id": 12345,
                  "title": "How to fix NullPointerException",
                  "link": "https://stackoverflow.com/questions/12345",
                  "score": 42,
                  "answer_count": 5,
                  "is_answered": true,
                  "creation_date": 1704067200,
                  "owner": { "display_name": "user123" }
                }
              ],
              "has_more": false,
              "quota_remaining": 299
            }
        """;

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(json)
                .addHeader("Content-Type", "application/json"));

        List<StackOverflowQuestion> questions = client.search("NullPointerException", "java");

        assertThat(questions).hasSize(1);
        StackOverflowQuestion q = questions.getFirst();

        assertThat(q.title()).isEqualTo("How to fix NullPointerException");
        assertThat(q.score()).isEqualTo(42);
        assertThat(q.answerCount()).isEqualTo(5);
        assertThat(q.isAnswered()).isTrue();
    }

    @Test
    void returnsEmptyListOnError() {
        mockServer.enqueue(new MockResponse().setResponseCode(500));

        List<StackOverflowQuestion> questions = client.search("anything", "java");

        assertThat(questions).isEmpty();
    }

    @Test
    void returnsEmptyListWhenNoItems() {
        String json = """
            {
              "items": [],
              "has_more": false,
              "quota_remaining": 299
            }
        """;

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(json)
                .addHeader("Content-Type", "application/json"));

        List<StackOverflowQuestion> questions = client.search("obscure error", "python");

        assertThat(questions).isEmpty();
    }
}