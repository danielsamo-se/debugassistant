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
    void extractsKeywordsFromExceptionType() {
        ParsedError error = ParsedError.builder()
                .exceptionType("NullPointerException")
                .message("something failed")
                .build();

        List<String> result = extractor.extract(error);

        assertThat(result).contains("nullpointerexception");
    }

    @Test
    void mergesDuplicateKeywords() {
        ParsedError error = ParsedError.builder()
                .exceptionType("TimeoutError")
                .message("timeout occurred here")
                .rootCause("Some deeper timeout issue")
                .build();

        List<String> result = extractor.extract(error);

        assertThat(result).contains("timeout");
    }

    @Test
    void filtersStopwords() {
        ParsedError error = ParsedError.builder()
                .exceptionType("SomeError")
                .message("the message failed for the user")
                .build();

        List<String> result = extractor.extract(error);

        assertThat(result).doesNotContain("the", "error", "failed", "for");
    }

    @Test
    void limitsOutputToFiveKeywords() {
        ParsedError error = ParsedError.builder()
                .exceptionType("OneError TwoError ThreeError FourError FiveError SixError SevenError")
                .build();

        List<String> result = extractor.extract(error);

        assertThat(result).hasSizeLessThanOrEqualTo(5);
    }

    @Test
    void returnsEmptyListForNullError() {
        List<String> result = extractor.extract(null);

        assertThat(result).isEmpty();
    }
}