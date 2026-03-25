package com.moveon.infra.exception;

/**
 * Exception for invalid argument errors.
 */
public class InvalidArgumentException extends BusinessException {

    public InvalidArgumentException(String field, String message) {
        super("INVALID_ARGUMENT", String.format("Invalid %s: %s", field, message));
    }

    public InvalidArgumentException(String message) {
        super("INVALID_ARGUMENT", message);
    }
}
