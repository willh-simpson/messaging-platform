package com.messaging.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

/**
 * Standard API envelope used by every endpoint across services.
 * <p>
 * Shape:
 *  { "success": true, "data": { ... }, "timestamp: "..." }
 *  { "success": false, "error": "...", "timestamp: "..." }
 *
 * @param <T> Payload type for successful response
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final String error;
    private final Instant timestamp;

    private ApiResponse(boolean success, T data, String error) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.timestamp = Instant.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
