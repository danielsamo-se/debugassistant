package com.debugassistant.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeRequest {

    @NotBlank(message = "Stack trace cannot be empty")
    @Size(min = 10, max = 50000,
            message = "Stack trace must be between 10 and 50000 characters")
    @Schema(description = "Stack trace to analyze",
            example = "java.lang.NullPointerException: Cannot invoke method\\n" +
                    "    at com.app.Service.process(Service.java:42)")
    private String stackTrace;
}