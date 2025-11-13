package com.debugassistant.backend.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PythonErrorParserTest {

    private PythonErrorParser parser;

    @BeforeEach
    void setUp() {
        KeywordExtractor keywordExtractor = new KeywordExtractor();
        RootCauseExtractor rootCauseExtractor = new RootCauseExtractor();
        parser = new PythonErrorParser(keywordExtractor, rootCauseExtractor);
    }

    @Test
    void shouldParseStandardPythonError() {
        String stackTrace = """
                Traceback (most recent call last):
                  File "script.py", line 10, in <module>
                    calculate()
                ZeroDivisionError: division by zero
                """;

        ParsedError result = parser.parse(stackTrace);

        assertThat(result.language()).isEqualTo("python");
        assertThat(result.exceptionType()).isEqualTo("ZeroDivisionError");
        assertThat(result.message()).isEqualTo("division by zero");
    }

    @Test
    void shouldHandleUnconventionalFormat() {
        String stackTrace = """
                File "script.py", line 1
                SyntaxError invalid syntax
                """;

        ParsedError result = parser.parse(stackTrace);

        assertThat(result.language()).isEqualTo("python");
        assertThat(result.exceptionType()).isEqualTo("UnknownPythonError");
        assertThat(result.message()).contains("SyntaxError invalid syntax");
    }

    @Test
    void shouldExtractKeywords() {
        String stackTrace = """
                Traceback (most recent call last):
                  File "app.py", line 5, in main
                ValueError: invalid literal for int
                """;

        ParsedError result = parser.parse(stackTrace);

        assertThat(result.keywords()).isNotEmpty();
    }
}