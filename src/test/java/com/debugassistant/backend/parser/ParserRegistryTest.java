package com.debugassistant.backend.parser;

import com.debugassistant.backend.exception.InvalidStackTraceException;
import com.debugassistant.backend.exception.UnsupportedLanguageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParserRegistryTest {

    @Mock
    private JavaErrorParser javaErrorParser;

    @Mock
    private PythonErrorParser pythonErrorParser;

    @InjectMocks
    private ParserRegistry parserRegistry;

    @Test
    void shouldRouteToJavaParser() {
        String javaTrace = "java.lang.NullPointerException at com.test.Main(Main.java:10)";

        when(javaErrorParser.parse(anyString()))
                .thenReturn(ParsedError.builder()
                        .language("java")
                        .exceptionType("NullPointerException")
                        .message("msg")
                        .keywords(Set.of("nullpointerexception"))
                        .rootCause("java.lang.NullPointerException: boom")
                        .stackTraceLines(2)
                        .build());

        ParsedError result = parserRegistry.parse(javaTrace);

        verify(javaErrorParser).parse(javaTrace);
        assertThat(result.language()).isEqualTo("java");
    }

    @Test
    void shouldRouteToPythonParser() {
        String pythonTrace = "Traceback (most recent call last):\n  File \"script.py\", line 1, in <module>\nValueError: bad";

        when(pythonErrorParser.parse(anyString()))
                .thenReturn(ParsedError.builder()
                        .language("python")
                        .exceptionType("ValueError")
                        .message("msg")
                        .keywords(Set.of("valueerror"))
                        .rootCause("ValueError: bad")
                        .stackTraceLines(3)
                        .build());

        ParsedError result = parserRegistry.parse(pythonTrace);

        verify(pythonErrorParser).parse(pythonTrace);
        assertThat(result.language()).isEqualTo("python");
    }

    @Test
    void shouldPreferJavaOnTie() {
        String mixed = """
                Traceback
                java.lang.Exception
                """;

        when(javaErrorParser.parse(anyString()))
                .thenReturn(ParsedError.builder()
                        .language("java")
                        .exceptionType("Exception")
                        .message("")
                        .keywords(Set.of())
                        .rootCause(null)
                        .stackTraceLines(2)
                        .build());

        ParsedError result = parserRegistry.parse(mixed);

        verify(javaErrorParser).parse(mixed);
        assertThat(result.language()).isEqualTo("java");
    }

    @Test
    void shouldThrowExceptionForEmptyInput() {
        assertThatThrownBy(() -> parserRegistry.parse(""))
                .isInstanceOf(InvalidStackTraceException.class);
    }

    @Test
    void shouldThrowExceptionForNullInput() {
        assertThatThrownBy(() -> parserRegistry.parse(null))
                .isInstanceOf(InvalidStackTraceException.class);
    }

    @Test
    void shouldThrowExceptionForUnknownLanguage() {
        String weirdText = "I am not a stack trace just some random text";

        assertThatThrownBy(() -> parserRegistry.parse(weirdText))
                .isInstanceOf(UnsupportedLanguageException.class);
    }
}
