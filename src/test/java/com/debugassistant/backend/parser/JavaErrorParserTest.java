package com.debugassistant.backend.parser;

import com.debugassistant.backend.exception.InvalidStackTraceException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class JavaErrorParserTest {

    private final JavaErrorParser parser = new JavaErrorParser();

    @Test
    void shouldRejectEmptyInput() {
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(InvalidStackTraceException.class)
                .hasMessageContaining("cannot be empty");
    }

    @Test
    void shouldRejectNullInput() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(InvalidStackTraceException.class)
                .hasMessageContaining("cannot be empty");
    }

    @Test
    void shouldParseSpringBootException() {
        String trace = """
        org.springframework.web.HttpRequestMethodNotSupportedException: Request method 'GET' not supported
            at org.springframework.web.servlet.mvc.Controller.handle
        """;

        ParsedError result = parser.parse(trace);

        assertThat(result.exceptionType()).contains("HttpRequestMethodNotSupportedException");
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

        assertThat(result.exceptionType()).isEqualTo("java.lang.RuntimeException");
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
        assertThat(result.exceptionType()).isEqualTo("java.lang.IllegalStateException");
        assertThat(result.message()).isEmpty();
    }
}
