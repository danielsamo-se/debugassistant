package com.debugassistant.backend.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaErrorParserTest {

    private JavaErrorParser parser;

    @BeforeEach
    void setUp() {
        KeywordExtractor keywordExtractor = new KeywordExtractor();
        RootCauseExtractor rootCauseExtractor = new RootCauseExtractor();
        parser = new JavaErrorParser(keywordExtractor, rootCauseExtractor);
    }

    @Test
    void shouldParseSpringBootException() {
        String trace = """
                org.springframework.web.HttpRequestMethodNotSupportedException: Request method 'GET' not supported
                    at org.springframework.web.servlet.mvc.Controller.handle
                """;

        ParsedError result = parser.parse(trace);

        assertThat(result.exceptionType()).isEqualTo("HttpRequestMethodNotSupportedException");
        assertThat(result.message()).contains("Request method 'GET' not supported");
    }

    @Test
    void shouldParseMultilineStackTrace() {
        String trace = """
                java.lang.RuntimeException: Database error
                    at com.app.Database.connect(Database.java:50)
                    at com.app.Service.init(Service.java:20)
                """;

        ParsedError result = parser.parse(trace);

        assertThat(result.exceptionType()).isEqualTo("RuntimeException");
        assertThat(result.message()).isEqualTo("Database error");
    }

    @Test
    void shouldParseExceptionWithoutMessage() {
        String trace = """
                java.lang.IllegalStateException
                    at com.app.Main.main(Main.java:10)
                """;

        ParsedError result = parser.parse(trace);

        assertThat(result.language()).isEqualTo("java");
        assertThat(result.exceptionType()).isEqualTo("IllegalStateException");
        assertThat(result.message()).isEmpty();
    }

    @Test
    void shouldExtractRootCause() {
        String trace = """
                java.lang.RuntimeException: Outer error
                    at com.app.Main.main(Main.java:10)
                Caused by: java.sql.SQLException: Connection failed
                    at com.db.Pool.connect(Pool.java:20)
                """;

        ParsedError result = parser.parse(trace);

        assertThat(result.exceptionType()).isEqualTo("RuntimeException");
        assertThat(result.rootCause()).isEqualTo("SQLException");
    }

    @Test
    void shouldExtractKeywords() {
        String trace = """
                java.lang.NullPointerException: Cannot invoke method on null object
                    at com.app.Service.process(Service.java:42)
                """;

        ParsedError result = parser.parse(trace);

        assertThat(result.keywords()).isNotEmpty();
    }
}