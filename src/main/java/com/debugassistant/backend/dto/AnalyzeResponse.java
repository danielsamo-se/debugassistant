package com.debugassistant.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalyzeResponse {

    private String language;
    private String exceptionType;
    private String message;
    private int score;
}