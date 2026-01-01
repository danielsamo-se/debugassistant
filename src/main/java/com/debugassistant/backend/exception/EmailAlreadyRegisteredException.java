package com.debugassistant.backend.exception;

/**
 * Thrown when an email address is already registered
 */
public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException(String message) {
        super(message);
    }
}
