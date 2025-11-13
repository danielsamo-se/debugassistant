package com.debugassistant.backend.exception.handler;

import com.debugassistant.backend.exception.ErrorResponse;
import com.debugassistant.backend.exception.InvalidStackTraceException;
import com.debugassistant.backend.exception.UnsupportedLanguageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Handles exceptions globally and returns error responses
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidStackTraceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStackTrace(InvalidStackTraceException ex) {
        log.warn("Invalid stack trace: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(UnsupportedLanguageException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedLanguage(UnsupportedLanguageException ex) {
        log.warn("Unsupported language: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("Validation error: {}", message);
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(message, Instant.now()));
    }
}
