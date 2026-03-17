package com.messaging.messagingservice.api.controller.exception;

import com.messaging.common.dto.ApiResponse;
import com.messaging.messagingservice.config.ApiMediaTypes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Centralized error handling for messaging services.
 */
@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    /**
     * Handles @Valid failures.
     *
     * @param ex Intercepted exception.
     * @return 400, map of field -> error message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed: " + errors));
    }

    /**
     * Failed membership check.
     *
     * @param ex Intercepted exception.
     * @return 403, generic message.
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(SecurityException ex) {
        log.warn("Channel access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("You are not a member of this channel"));
    }

    /**
     * Operation within business logic is invalid.
     *
     * @param ex Intercepted exception.
     * @return 400, exception message.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
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
