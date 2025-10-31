package com.debugassistant.backend.exceptions;

public class InvalidStackTraceException extends RuntimeException {
    public InvalidStackTraceException(String message) {
        super(message);
    }
}