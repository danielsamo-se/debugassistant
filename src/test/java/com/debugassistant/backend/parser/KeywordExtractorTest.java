package com.debugassistant.backend.parser;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordExtractorTest {

    private KeywordExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new KeywordExtractor();
    }

    @Test
    void extractsKeywordsFromExceptionTypeWithHighestWeight() {
        ParsedError error = ParsedError.builder()
                .exceptionType("NullPointerException SomethingFailed")
                .message("minor details")
                .rootCause(null)
                .build();

        List<String> result = extractor.extract(error);

        assertThat(result).contains("nullpointerexception", "somethingfailed");
    }

    @Test
    void mergesDuplicateKeywordsAndIncreasesScore() {
        ParsedError error = ParsedError.builder()
                .exceptionType("TimeoutError")
                .message("timeout occurred here")
                .rootCause("Some deeper timeout issue")
                .build();

        List<String> result = extractor.extract(error);

        assertThat(result).contains("timeout");
    }

    @Test
    void filtersStaticStopwords() {
        ParsedError error = ParsedError.builder()
                .exceptionType("Error")
                .message("the message failed for the user")
                .build();

        List<String> result = extractor.extract(error);

        assertThat(result).doesNotContain("the", "error", "failed");
    }

    @Test
    void filtersDynamicStopwords() {
        ParsedError error = ParsedError.builder()
                .exceptionType("12345")
                .message("/usr/home/app/Main.java")
                .rootCause("ABCDEF123")
                .build();

        List<String> result = extractor.extract(error);

        assertThat(result).contains("usr", "home", "app");
    }

    @Test
    void limitsOutputToFiveKeywords() {
        ParsedError error = ParsedError.builder()
                .exceptionType("One Two Three Four Five Six Seven")
                .build();

        List<String> result = extractor.extract(error);

        assertThat(result).hasSize(5);
    }
}