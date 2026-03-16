package com.messaging.apigateway.services;

import com.messaging.apigateway.api.dto.response.UserResponse;
import com.messaging.apigateway.domain.repository.UserRepository;
import com.messaging.apigateway.security.JwtTokenProvider;
import com.messaging.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Manages users and user information.
 */
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepo;
    private final JwtTokenProvider tokenProvider;

    /**
     * Get user by UUID.
     *
     * @param userId Unique user ID.
     * @return User information if exists.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        return userRepo.findById(userId)
                .map(UserResponse::toResponse)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User", "id", userId
                        )
                );
    }

    /**
     * Get user from JWT.
     *
     * @param bearerToken Token from Authorization header.
     * @return User information if exists.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserFromToken(String bearerToken) {
        String token = bearerToken.startsWith("Bearer ")
                ? bearerToken.substring(7)
                : bearerToken;
        UUID userId = tokenProvider.getUserIdFromToken(token);

        return getUserById(userId);
    }
}
