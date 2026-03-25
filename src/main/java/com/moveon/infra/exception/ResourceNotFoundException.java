package com.moveon.infra.exception;

import lombok.Getter;

/**
 * Exception for resource not found errors.
 */
@Getter
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceType, Long id) {
        super("RESOURCE_NOT_FOUND", String.format("%s not found with id: %d", resourceType, id));
    }

    public ResourceNotFoundException(String resourceType, String field, Object value) {
        super("RESOURCE_NOT_FOUND", String.format("%s not found with %s: %s", resourceType, field, value));
    }
}
