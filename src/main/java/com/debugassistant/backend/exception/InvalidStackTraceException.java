package com.debugassistant.backend.exception;

/**
 * Thrown when the stack trace input is empty or invalid
 */
public class InvalidStackTraceException extends RuntimeException {
    public InvalidStackTraceException(String message) {
        super(message);
    }
}