package com.moveon.infra.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Unified error response structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private Instant timestamp;
    private Integer status;
    private String error;
    private String code;
    private String message;
    private String path;

    public static ErrorResponse of(Integer status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, null, message, path);
    }

    public static ErrorResponse of(Integer status, String error, String code, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, code, message, path);
    }
}
