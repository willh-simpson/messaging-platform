package com.messaging.apigateway.api.controller;

import com.messaging.apigateway.api.dto.request.LoginRequest;
import com.messaging.apigateway.api.dto.request.RegisterRequest;
import com.messaging.apigateway.api.dto.response.AuthResponse;
import com.messaging.apigateway.config.ApiMediaTypes;
import com.messaging.apigateway.services.AuthService;
import com.messaging.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints for user registration and login.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * Register user.
     *
     * @param request Requesting user information.
     * @return Auth information if user is successfully registered.
     */
    @PostMapping(value = "/register", produces = ApiMediaTypes.V1_JSON)
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse auth = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(auth));
    }

    /**
     * Log in user.
     *
     * @param request Requesting user information.
     * @return Auth information if user is successfully validated.
     */
    @PostMapping(value = "/login", produces = ApiMediaTypes.V1_JSON)
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse auth = authService.login(request);

        return ResponseEntity.ok(ApiResponse.success(auth));
    }
}
