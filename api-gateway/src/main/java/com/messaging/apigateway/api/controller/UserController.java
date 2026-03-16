package com.messaging.apigateway.api.controller;

import com.messaging.apigateway.api.dto.response.UserResponse;
import com.messaging.apigateway.config.ApiMediaTypes;
import com.messaging.apigateway.services.UserService;
import com.messaging.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Public endpoints for user management.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * Get authenticated user's own profile.
     *
     * @param authHeader User auth information.
     * @return 200, user's own information.
     */
    @GetMapping(value = "/me", produces = ApiMediaTypes.V1_JSON)
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        UserResponse user = userService.getUserFromToken(authHeader);

        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * Get user's public profile by ID.
     *
     * @param userId User's unique ID.
     * @return 200, user's public information.
     */
    @GetMapping(value = "/{user_id}", produces = ApiMediaTypes.V1_JSON)
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable("user_id") UUID userId) {
        UserResponse user = userService.getUserById(userId);

        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
