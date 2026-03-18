package com.messaging.deliveryservice.security;

import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Runs on every inbound STOMP message.
 */
@Component
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {
    @Override
    public Message<?> preSend(
            @Nonnull Message<?> message,
            @Nonnull MessageChannel channel
    ) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            // only need to act on CONNECT frames
            return message;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            log.warn("STOMP CONNECT has no session attributes and will be rejected");

            throw new IllegalStateException("No session attributes on CONNECT frame");
        }

        Object principal = sessionAttributes.get(JwtHandshakeInterceptor.AUTHENTICATED_USER_ATTR);
        if (!(principal instanceof AuthenticatedUser user)) {
            log.warn("STOMP CONNECT is missing authenticated user and will be rejected");

            throw new IllegalStateException("No authenticated user in WebSocket session");
        }

        accessor.setUser(new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
        log.debug("STOMP CONNECT authenticated for username={}, userId={}", user.username(), user.userId());

        return message;
    }
}
