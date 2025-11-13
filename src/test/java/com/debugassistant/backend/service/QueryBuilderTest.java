package com.debugassistant.backend.service;

import com.debugassistant.backend.parser.ParsedError;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class QueryBuilderTest {

    private final QueryBuilder queryBuilder = new QueryBuilder();

    @Test
    void buildsQueryWithExceptionTypeAndKeywords() {
        ParsedError error = ParsedError.builder()
                .exceptionType("NullPointerException")
                .keywords(Set.of("invoke", "object"))
                .build();

        String query = queryBuilder.buildSmartQuery(error);

        assertThat(query).contains("NullPointerException");
        assertThat(query).contains("invoke");
        assertThat(query).contains("object");
        assertThat(query).endsWith("in:title,body");
    }

    @Test
    void limitsKeywordsToThree() {
        ParsedError error = ParsedError.builder()
                .exceptionType("IllegalArgumentException")
                .keywords(Set.of("alpha", "beta", "gamma", "delta", "epsilon"))
                .build();

        String query = queryBuilder.buildSmartQuery(error);

        // exceptionType + max 3 keywords + in:title,body
        String[] parts = query.replace(" in:title,body", "").split(" ");
        assertThat(parts.length).isLessThanOrEqualTo(4);
    }

    @Test
    void cleansInvalidCharacters() {
        ParsedError error = ParsedError.builder()
                .exceptionType("NullPointerException")
                .keywords(Set.of("!!!inv@oke", "123read"))
                .build();

        String query = queryBuilder.buildSmartQuery(error);

        assertThat(query).contains("invoke");
        assertThat(query).contains("read");
        assertThat(query).doesNotContain("!");
        assertThat(query).doesNotContain("@");
    }

    @Test
    void filtersStopwords() {
        ParsedError error = ParsedError.builder()
                .exceptionType("NullPointerException")
                .keywords(Set.of("cannot", "the", "validword"))
                .build();

        String query = queryBuilder.buildSmartQuery(error);

        assertThat(query).contains("validword");
        assertThat(query).doesNotContain("cannot");
        assertThat(query).doesNotContain(" the ");
    }

    @Test
    void handlesEmptyKeywords() {
        ParsedError error = ParsedError.builder()
                .exceptionType("IndexOutOfBoundsException")
                .keywords(Set.of())
                .build();

        String query = queryBuilder.buildSmartQuery(error);

        assertThat(query).isEqualTo("IndexOutOfBoundsException in:title,body");
    }

    @Test
    void handlesMissingExceptionType() {
        ParsedError error = ParsedError.builder()
                .keywords(Set.of("timeout", "connection"))
                .build();

        String query = queryBuilder.buildSmartQuery(error);

        assertThat(query).contains("timeout");
        assertThat(query).endsWith("in:title,body");
    }

    @Test
    void returnsDefaultForEmptyError() {
        ParsedError error = ParsedError.builder().build();

        String query = queryBuilder.buildSmartQuery(error);

        assertThat(query).isEqualTo("exception in:title,body");
    }
}