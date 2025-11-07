package com.debugassistant.backend.parser;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class RootCauseExtractorTest {

    private final RootCauseExtractor extractor = new RootCauseExtractor();

    @Test
    void returnsNullWhenNoRootCauseFound() {
        String trace = "NullPointerException: test\nat line 1";
        assertThat(extractor.extractRootCauseLine(trace)).isNull();
    }

    @Test
    void extractsSimpleCausedBy() {
        String trace = """
                Something failed
                Caused by: IllegalArgumentException: Wrong value
                """;

        assertThat(extractor.extractRootCauseLine(trace))
                .isEqualTo("IllegalArgumentException: Wrong value");
    }

    @Test
    void extractsDeepestCausedBy() {
        String trace = """
                Error creating bean
                Caused by: BeanCreationException: x
                Caused by: MappingException: Could not map Address
                """;

        assertThat(extractor.extractRootCauseLine(trace))
                .isEqualTo("MappingException: Could not map Address");
    }

    @Test
    void extractsNestedExceptionIs() {
        String trace = """
                Error
                nested exception is org.hibernate.LazyInitializationException: Could not initialize proxy
                """;

        assertThat(extractor.extractRootCauseLine(trace))
                .isEqualTo("org.hibernate.LazyInitializationException: Could not initialize proxy");
    }

    @Test
    void extractsPythonStyleRootCause() {
        String trace = """
                AttributeError: 'NoneType' object has no attribute 'name'
                During handling of the above exception, another exception occurred:
                TypeError: unsupported operand type(s)
                """;

        // Python teilweise braucht etwas smartere Logik,
        // aber wir können die letzte Exception erkennen:
        assertThat(extractor.extractRootCauseLine(trace))
                .isEqualTo("TypeError: unsupported operand type(s)");
    }
}