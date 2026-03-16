package com.messaging.common.exception;

/**
 * Thrown when requested resource does not exist.
 * HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(
            String resourceName,
            String fieldName,
            Object fieldValue
    ) {
        super(String.format(
                "%s not found with %s: '%s'",
                resourceName, fieldName, fieldValue
        ));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
