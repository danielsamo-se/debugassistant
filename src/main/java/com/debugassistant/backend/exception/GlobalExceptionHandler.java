package com.debugassistant.backend.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles exceptions globally and returns error responses
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // thrown when stack trace is malformed or unreadable
    @ExceptionHandler(InvalidStackTraceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStackTrace(InvalidStackTraceException ex) {
        log.warn("Invalid stack trace: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(ex.getMessage(), Instant.now()));
    }

    // thrown when language could not be mapped
    @ExceptionHandler(UnsupportedLanguageException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedLanguage(UnsupportedLanguageException ex) {
        log.warn("Unsupported language: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(ex.getMessage(), Instant.now()));
    }

    // validation errors from @Valid DTOs
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Validation error");

        log.warn("Validation error: {}", message);
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(message, Instant.now()));
    }

    // login failure (wrong email/password)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials");
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Invalid email or password", Instant.now()));
    }

    // account disabled
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex) {
        log.warn("Account disabled");
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Account is disabled", Instant.now()));
    }

    // user authenticated but no permission
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied");
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("Access denied", Instant.now()));
    }

    // fallback for unexpected errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An unexpected error occurred", Instant.now()));
    }

    // static resource not found
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "message", "Not found",
                "path", ex.getResourcePath()
        ));
    }

    // invalid JSON or unreadable request body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Bad JSON request: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("Invalid JSON payload", Instant.now()));
    }

    // wrong HTTP method
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse("Method not allowed", Instant.now()));
    }

    // wrong content-type
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported media type: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorResponse("Unsupported media type", Instant.now()));
    }

    // violations for params/path variables
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .distinct()
                .collect(Collectors.joining(", "));
        log.warn("Constraint violation: {}", message);
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(message.isBlank() ? "Validation failed" : message, Instant.now()));
    }
}