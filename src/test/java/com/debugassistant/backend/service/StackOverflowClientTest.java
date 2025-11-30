package com.debugassistant.backend.service;

import com.debugassistant.backend.dto.stackoverflow.StackOverflowQuestion;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
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
    void javaHappyPath() throws Exception {
        String json = """
        {
          "items": [
            {
              "question_id": 123,
              "title": "How to fix BeanCreationException?",
              "link": "https://stackoverflow.com/q/123",
              "score": 100,
              "answer_count": 3,
              "is_answered": true,
              "creation_date": 1700000000,
              "owner": { "display_name": "JohnDoe" }
            }
          ],
          "quota_remaining": 300
        }
        """;

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(json));

        List<StackOverflowQuestion> result = client.search("BeanCreationException", "java");

        assertThat(result).hasSize(1);
        StackOverflowQuestion q = result.getFirst();

        assertThat(q.title()).contains("BeanCreationException");
        assertThat(q.questionId()).isEqualTo(123);
        assertThat(q.score()).isEqualTo(100);
        assertThat(q.answerCount()).isEqualTo(3);
        assertThat(q.owner().displayName()).isEqualTo("JohnDoe");

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getRequestUrl().queryParameter("tagged")).isEqualTo("java");
    }

    @Test
    void pythonHappyPath() throws Exception {
        String json = """
        {
          "items": [
            {
              "question_id": 456,
              "title": "IndexError: list index out of range",
              "link": "https://stackoverflow.com/q/456",
              "score": 50,
              "answer_count": 1,
              "is_answered": true,
              "creation_date": 1600000000,
              "owner": { "display_name": "PyGuru" }
            }
          ],
          "quota_remaining": 250
        }
        """;

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(json));

        List<StackOverflowQuestion> result = client.search("IndexError", "python");

        assertThat(result).hasSize(1);
        StackOverflowQuestion q = result.getFirst();

        assertThat(q.title()).contains("IndexError");
        assertThat(q.owner().displayName()).isEqualTo("PyGuru");

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getRequestUrl().queryParameter("tagged")).isEqualTo("python");
    }

    @Test
    void emptyTagWhenLanguageUnknown() throws Exception {
        String json = """
        {
          "items": [],
          "quota_remaining": 100
        }
        """;

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(json));

        List<StackOverflowQuestion> result = client.search("anything", "rust");

        assertThat(result).isEmpty();

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getRequestUrl().queryParameter("tagged")).isEqualTo("");
    }

    @Test
    void returnsEmptyListWhenItemsNull() {
        String json = """
        {
          "items": null,
          "quota_remaining": 200
        }
        """;

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(json));

        assertThat(client.search("test", "java")).isEmpty();
    }

    @Test
    void returnsEmptyListOnApiError() {
        mockServer.enqueue(new MockResponse().setResponseCode(429));
        assertThat(client.search("test", "java")).isEmpty();

        mockServer.enqueue(new MockResponse().setResponseCode(403));
        assertThat(client.search("test", "java")).isEmpty();

        mockServer.enqueue(new MockResponse().setResponseCode(500));
        assertThat(client.search("test", "java")).isEmpty();
    }
}