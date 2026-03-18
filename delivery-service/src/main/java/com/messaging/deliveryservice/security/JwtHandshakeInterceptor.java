package com.messaging.deliveryservice.security;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

/**
 * Validates JWT before WebSocket connection is accepted, during HTTP upgrade handshake.
 * <p>
 * Token will be sent via query parameter.
 * Query parameter will be encrypted in transit in production and
 * be redacted by load balancer/reverse proxy to prevent being exposed in logs.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtTokenProvider tokenProvider;

    // used to store authenticated user in WebSocket session attribute
    public static final String AUTHENTICATED_USER_ATTR = "authenticatedUser";

    @Override
    public boolean beforeHandshake(
            @Nonnull ServerHttpRequest request,
            @Nonnull ServerHttpResponse response,
            @Nonnull WebSocketHandler wsHandler,
            @Nonnull Map<String, Object> attributes
    ) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            log.warn("Unexpected request type during WebSocket handshake: {}", request.getClass().getName());

            return false;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        String token = httpRequest.getParameter("token");

        if (!StringUtils.hasText(token)) {
            log.warn("WebSocket handshake rejected: no token parameter");

            return false;
        }
        if (!tokenProvider.validateToken(token)) {
            log.warn("WebSocket handshake rejected: invalid or expired token");

            return false;
        }

        UUID userId = tokenProvider.getUserIdFromToken(token);
        String username = tokenProvider.getUsernameFromToken(token);
        attributes.put(AUTHENTICATED_USER_ATTR, new AuthenticatedUser(userId, username));
        log.debug("WebSocket handshake accepted for username={}, userId={}", username, userId);

        return true;
    }

    @Override
    public void afterHandshake(
            @Nonnull ServerHttpRequest request,
            @Nonnull ServerHttpResponse response,
            @Nonnull WebSocketHandler wsHandler,
            Exception exception
    ) {
        // no post-handshake action needed
    }
}
