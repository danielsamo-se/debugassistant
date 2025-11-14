package com.debugassistant.backend.dto.stackoverflow;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Stack Overflow question from the Search API
 */
public record StackOverflowQuestion(
        @JsonProperty("question_id")
        Long questionId,

        String title,
        String link,
        int score,

        @JsonProperty("answer_count")
        int answerCount,

        @JsonProperty("is_answered")
        boolean isAnswered,

        @JsonProperty("creation_date")
        Long creationDate,

        Owner owner
) {
    public record Owner(
            @JsonProperty("display_name")
            String displayName
    ) {}
}