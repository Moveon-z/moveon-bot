package com.moveon.infra.exception;

import lombok.Getter;

/**
 * Base exception for business logic errors.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final Object[] args;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.args = null;
    }

    public BusinessException(String code, String message, Object[] args) {
        super(message);
        this.code = code;
        this.args = args;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.args = null;
    }
}
