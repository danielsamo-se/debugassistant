package com.debugassistant.backend.parser;

import com.debugassistant.backend.exception.InvalidStackTraceException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PythonErrorParserTest {

    private final PythonErrorParser parser = new PythonErrorParser();

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
        // test fallback
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
    void shouldRejectEmptyInput() {
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(InvalidStackTraceException.class)
                .hasMessageContaining("cannot be empty");
    }
}