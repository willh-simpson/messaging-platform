package com.messaging.apigateway.service;

import com.messaging.apigateway.api.dto.request.LoginRequest;
import com.messaging.apigateway.api.dto.request.RegisterRequest;
import com.messaging.apigateway.api.dto.response.AuthResponse;
import com.messaging.apigateway.domain.model.User;
import com.messaging.apigateway.domain.repository.UserRepository;
import com.messaging.apigateway.security.JwtTokenProvider;
import com.messaging.common.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles user registration and authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authManager;

    /**
     * Create new user and provide auth token.
     *
     * @param request User username, email, password, display name.
     * @return Auth token and user details.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepo.existsByUsername(request.username())) {
            throw new IllegalArgumentException(
                    String.format("Username '%s' is already taken", request.username())
            );
        }
        if (userRepo.existsByEmail(request.email())) {
            throw new IllegalArgumentException(
                    String.format("Email '%s' is already registered", request.email())
            );
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName() != null
                        ? request.displayName()
                        : request.username()
                )
                .build();
        user = userRepo.save(user);
        log.info("Registered new user: {} ({})", user.getUsername(), user.getId());

        String token = tokenProvider.generateToken(user.getId(), user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .build();
    }

    /**
     * Log user into platform and provide auth token.
     *
     * @param request Username and password.
     * @return Auth token and user details.
     */
    public AuthResponse login(LoginRequest request) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(), request.password()
                )
        );

        User user = userRepo.findByUsername(request.username())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User", "username", request.username()
                        )
                );
        String token = tokenProvider.generateToken(user.getId(), user.getUsername());
        log.info("User logged in: {}", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .build();
    }
}
