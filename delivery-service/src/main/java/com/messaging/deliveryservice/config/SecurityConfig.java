package com.messaging.deliveryservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security config for Delivery Service. Permits the /ws path to allow WebSocket communication.
 *
 * @apiNote This service has no REST API, meaning there are no controller endpoints to protect with JWT filter chain.
 * Authentication is handled at the WebSocket layer.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Permits /ws endpoint, otherwise Spring Security rejects upgrade request.
     * Authentication is handled at STOMP level rather than HTTP level.
     *
     * @param http HTTP security protocol.
     * @return Configured SecurityFilterChain.
     * @throws Exception Failure to configure HTTP security rules.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }
}
