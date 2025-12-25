package com.debugassistant.backend.service;

import com.debugassistant.backend.parser.ParsedError;
import org.junit.jupiter.api.Test;

import java.util.List;
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

        String query = queryBuilder.buildSmartQuery(error, null);

        assertThat(query).contains("NullPointerException");
        assertThat(query).contains("invoke");
    }

    @Test
    void limitsKeywordsToThree() {
        ParsedError error = ParsedError.builder()
                .exceptionType("Ex")
                .keywords(Set.of("alpha", "bravo", "charlie", "delta", "echo"))
                .build();

        String query = queryBuilder.buildSmartQuery(error, null);

        int tokens = query.split("\\s+").length;
        assertThat(tokens).isLessThanOrEqualTo(5);
    }

    @Test
    void cleansInvalidCharacters() {
        ParsedError error = ParsedError.builder()
                .exceptionType("E")
                .keywords(Set.of("!word"))
                .build();

        assertThat(queryBuilder.buildSmartQuery(error, null)).contains("word");
    }

    @Test
    void filtersStopwords() {
        ParsedError error = ParsedError.builder()
                .exceptionType("E")
                .keywords(Set.of("the", "valid"))
                .build();

        assertThat(queryBuilder.buildSmartQuery(error, null))
                .doesNotContain("the")
                .contains("valid");
    }

    @Test
    void returnsDefaultForEmptyError() {
        ParsedError error = ParsedError.builder().build();
        assertThat(queryBuilder.buildSmartQuery(error, null)).isEqualTo("exception in:title,body");
    }

    @Test
    void prioritizesRootCauseOverExceptionType() {
        ParsedError error = ParsedError.builder()
                .exceptionType("BeanCreationException")
                .rootCause("java.sql.SQLException: Connection refused")
                .keywords(Set.of("bean"))
                .build();

        String query = queryBuilder.buildSmartQuery(error, null);

        assertThat(query).contains("SQLException");
        assertThat(query).doesNotContain("BeanCreationException");
    }

    @Test
    void extractsCorrectLibraryFromStandardPackage() {
        ParsedError error = ParsedError.builder()
                .exceptionType("org.hibernate.exception.ConstraintViolationException")
                .build();

        String query = queryBuilder.buildSmartQuery(error, null);

        assertThat(query).contains("hibernate");
    }

    @Test
    void extractsDeepLibraryFromUmbrellaOrg() {
        ParsedError error = ParsedError.builder()
                .exceptionType("org.apache.kafka.common.errors.SerializationException")
                .build();

        String query = queryBuilder.buildSmartQuery(error, null);

        assertThat(query).contains("kafka");
        assertThat(query).doesNotContain("apache");
    }

    @Test
    void extractsSpringframeworkAsLibrary() {
        ParsedError error = ParsedError.builder()
                .exceptionType("org.springframework.beans.factory.BeanCreationException")
                .build();

        String query = queryBuilder.buildSmartQuery(error, null);

        assertThat(query).contains("springframework");
        assertThat(query).doesNotContain("beans");
        assertThat(query).contains("BeanCreationException");
    }

    @Test
    void buildStackOverflowQueries_sanitizesOperatorNoise() {
        ParsedError parsed = ParsedError.builder()
                .language("java")
                .exceptionType("NullPointerException")
                .message("Cannot invoke \"x\" because \"y\" is null in:title,body")
                .build();

        List<String> queries = queryBuilder.buildStackOverflowQueries(parsed, null);

        assertThat(queries.get(0)).doesNotContain("in:title,body");
    }

    @Test
    void buildStackOverflowQueries_extractsAndQuotesStrongPhrase() {
        ParsedError parsed = ParsedError.builder()
                .language("java")
                .exceptionType("NullPointerException")
                .message("Cannot invoke \"foo\" because \"bar\" is null")
                .build();

        List<String> queries = queryBuilder.buildStackOverflowQueries(parsed, null);

        assertThat(queries.get(1)).isEqualTo("\"Cannot invoke\"");
        assertThat(queries.get(0)).contains("Cannot invoke");
        assertThat(queries.get(0)).contains("\"Cannot invoke\"");
    }
}