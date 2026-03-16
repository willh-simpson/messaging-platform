package com.messaging.apigateway.config;

import com.messaging.apigateway.domain.repository.UserRepository;
import com.messaging.apigateway.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Optional;

/**
 * Spring Security configuration. Pathway for user auth.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter authFilter;
    private final UserRepository userRepo;

    /**
     * Designs and configures security filter chain for authentication validation and authorization rules.
     *
     * @param http HTTP security protocol.
     * @return Configured SecurityFilterChain.
     * @throws Exception Failure to configure HTTP security rules.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF is not needed for JWT APIs
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/channels").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Loads user by username for Spring Security to use to verify credentials and load authorities.
     * @return Username fetched from DB.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepo.findByUsername(username)
                .map(user -> User
                        .withUsername(user.getUsername())
                        .password(user.getPasswordHash())
                        .roles("USER")
                        .build()
                )
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + username)
                );
    }

    /**
     * Wire together UserDetailsService and PasswordEncoder for proper login flow.
     * @return DaoAuthenticationProvider wired to UserDetailsService and PasswordEncoder.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    /**
     * Used by AuthService to authenticate login attempts.
     *
     * @param config Access Spring Boot's autoconfigured auth manager.
     * @return Configured auth manager.
     * @throws Exception Failure of Spring Boot to create auth manager.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCrypt password encoder.
     *
     * @return New BCryptPasswordEncoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Tells Spring Data JPA who current user is.
     *
     * @return Authenticated user's ID as UUID string.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated()) {
                return Optional.of("system");
            }

            return Optional.of(auth.getName());
        };
    }
}
