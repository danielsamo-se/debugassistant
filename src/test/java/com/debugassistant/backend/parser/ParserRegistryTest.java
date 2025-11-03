package com.debugassistant.backend.parser;

import com.debugassistant.backend.exception.UnsupportedLanguageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        String javaTrace = "java.lang.NullPointerException at com.test.Main.java:10";

        when(javaErrorParser.parse(anyString()))
                .thenReturn(new ParsedError("java", "NPE", "msg"));

        ParsedError result = parserRegistry.parse(javaTrace);

        verify(javaErrorParser).parse(javaTrace);
        assertThat(result.language()).isEqualTo("java");
    }

    @Test
    void shouldRouteToPythonParser() {
        String pythonTrace = "Traceback (most recent call last):\nFile script.py...";

        when(pythonErrorParser.parse(anyString()))
                .thenReturn(new ParsedError("python", "Error", "msg"));

        ParsedError result = parserRegistry.parse(pythonTrace);

        verify(pythonErrorParser).parse(pythonTrace);
        assertThat(result.language()).isEqualTo("python");
    }

    @Test
    void shouldThrowExceptionForUnknownLanguage() {
        String weirdText = "I am not a stack trace just some random text";

        assertThatThrownBy(() -> parserRegistry.parse(weirdText))
                .isInstanceOf(UnsupportedLanguageException.class)
                .hasMessageContaining("not supported yet");
    }
}