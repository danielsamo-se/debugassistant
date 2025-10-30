package com.debugassistant.backend.parser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaErrorParserTest {

    private final JavaErrorParser parser = new JavaErrorParser();

    @Test
    void shouldParseExceptionAndMessage() {
        String trace = "java.lang.NullPointerException: Something bad happened\n"
                + "    at com.app.Main.main(Main.java:10)";

        ParsedError result = parser.parse(trace);

        assertThat(result.language()).isEqualTo("java");
        assertThat(result.exceptionType()).isEqualTo("java.lang.NullPointerException");
        assertThat(result.message()).isEqualTo("Something bad happened");
    }

    @Test
    void shouldHandleExceptionWithoutMessage() {
        String trace = "java.lang.IllegalStateException\n"
                + "    at com.app.Main.main(Main.java:10)";

        ParsedError result = parser.parse(trace);

        assertThat(result.exceptionType()).isEqualTo("java.lang.IllegalStateException");
        assertThat(result.message()).isEmpty();
    }

    @Test
    void shouldHandleEmptyInput() {
        ParsedError result = parser.parse("");

        assertThat(result.language()).isEqualTo("unknown");
        assertThat(result.exceptionType()).isEqualTo("unknown");
        assertThat(result.message()).isEqualTo("empty stacktrace");
    }
}
