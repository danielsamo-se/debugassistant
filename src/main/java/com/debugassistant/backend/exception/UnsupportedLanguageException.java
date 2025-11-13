package com.debugassistant.backend.exception;

/**
 * Thrown when the detected language is not supported
 */
public class UnsupportedLanguageException extends RuntimeException {
    public UnsupportedLanguageException(String message) {
        super(message);
    }
}