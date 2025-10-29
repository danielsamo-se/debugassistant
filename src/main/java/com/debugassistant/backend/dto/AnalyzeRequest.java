package com.debugassistant.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnalyzeRequest {

    @NotBlank(message = "stackTrace must not be empty")
    private String stackTrace;
}