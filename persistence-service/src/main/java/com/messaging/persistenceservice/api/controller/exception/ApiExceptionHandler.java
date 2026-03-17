package com.messaging.persistenceservice.api.controller.exception;

import com.messaging.common.dto.ApiResponse;
import com.messaging.common.exception.ResourceNotFoundException;
import com.messaging.persistenceservice.config.ApiMediaTypes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;

/**
 * Centralized error handling for persistence services.
 */
@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    /**
     * Resource not found.
     *
     * @param ex Intercepted exception.
     * @return 404, exception message.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.debug("Resource not found: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Unsupported API version.
     *
     * @param ex Intercepted exception.
     * @return 406, list of supported API versions.
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedVersion(
            HttpMediaTypeNotAcceptableException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_ACCEPTABLE)
                .body(ApiResponse.error(
                        "Unsupported API version. Set Accept: " + Arrays.toString(ApiMediaTypes.SUPPORTED_VERSIONS)
                ));
    }

    /**
     * Fallback for unexpected or untracked exceptions.
     *
     * @param ex Intercepted exception.
     * @return 500, generic message. Don't leak internal details to callers.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
    }
}
