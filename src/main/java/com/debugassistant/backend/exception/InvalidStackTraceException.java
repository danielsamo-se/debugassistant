package com.debugassistant.backend.exception;

public class InvalidStackTraceException extends RuntimeException {
    public InvalidStackTraceException(String message) {
        super(message);
    }
}