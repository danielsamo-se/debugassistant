package com.debugassistant.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request containing a stack trace to analyze
 */
public record AnalyzeRequest(

        @NotBlank(message = "Stack trace cannot be empty")
        @Size(min = 10, max = 50000, message = "Stack trace must be between 10 and 50000 characters")
        @Schema(description = "Stack trace to analyze",
                example = "java.lang.NullPointerException: Cannot invoke method\n" +
                        "    at com.app.Service.process(Service.java:42)")
        String stackTrace

) {}