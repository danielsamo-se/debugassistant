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
                .message("Cannot invoke because null")
                .keywords(Set.of("invoke", "null", "object"))
                .build();

        String query = queryBuilder.buildSmartQuery(error);

        assertThat(query)
                .isEqualTo("NullPointerException invoke object in:title,body");
    }

    @Test
    void limitsKeywordsToThree() {
        ParsedError error = ParsedError.builder()
                .exceptionType("IllegalArgumentException")
                .keywords(Set.of("foo", "bar", "baz", "extra"))
                .build();

        String query = queryBuilder.buildSmartQuery(error);

        // only 3 keywords allowed
        long count = query.split(" ").length;
        assertThat(count).isLessThanOrEqualTo(1 + 3 + 1); // exception + 3 keywords + in:title,body
    }

    @Test
    void cleansInvalidCharactersAndStopwords() {
        ParsedError error = ParsedError.builder()
                .exceptionType("NullPointerException")
                .keywords(Set.of("cannot", "!!!inv@oke", "123read"))
                .build();

        String query = queryBuilder.buildSmartQuery(error);

        assertThat(query)
                .isEqualTo("NullPointerException invoke read in:title,body");
    }

    @Test
    void handlesEmptyKeywords() {
        ParsedError error = ParsedError.builder()
                .exceptionType("IndexOutOfBoundsException")
                .keywords(Set.of())
                .build();

        String query = queryBuilder.buildSmartQuery(error);

        assertThat(query)
                .isEqualTo("IndexOutOfBoundsException in:title,body");
    }
}