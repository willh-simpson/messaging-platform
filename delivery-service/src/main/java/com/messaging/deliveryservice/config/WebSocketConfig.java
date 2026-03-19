package com.messaging.deliveryservice.config;

import com.messaging.deliveryservice.security.JwtHandshakeInterceptor;
import com.messaging.deliveryservice.security.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configures STOMP over WebSocket message broker.
 * Uses RabbitMQ to fan out messages to all connected instances to WebSocket.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Value("${rabbitmq.host:}")
    private String rabbitmqHost;
    @Value("${rabbitmq.stomp.port:61613}")
    private int rabbitmqStompPort;
    @Value("${rabbitmq.username:guest}")
    private String rabbitmqUsername;
    @Value("${rabbitmq.password:guest}")
    private String rabbitmqPassword;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if (rabbitmqHost != null && !rabbitmqHost.isBlank()) {
            registry.enableStompBrokerRelay("/topic")
                    .setRelayHost(rabbitmqHost)
                    .setRelayPort(rabbitmqStompPort)
                    .setClientLogin(rabbitmqUsername)
                    .setClientPasscode(rabbitmqPassword)
                    .setSystemLogin(rabbitmqUsername)
                    .setSystemPasscode(rabbitmqPassword)
                    .setAutoStartup(true);
        } else {
            // RabbitMQ isn't needed for local development
            registry.enableSimpleBroker("/topic");
        }

        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
