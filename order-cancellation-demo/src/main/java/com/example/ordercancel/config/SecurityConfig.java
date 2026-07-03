package com.example.ordercancel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.security.cors.allowed-origins:http://localhost:8080}")
    private String allowedOriginsRaw;

    /**
     * Security filter chain:
     * - CORS configured with explicit allowed origins (no wildcard)
     * - CSRF disabled (stateless REST API)
     * - Stateless sessions (no server-side session state)
     * - All /api/** endpoints are publicly accessible in this demo.
     *   In a real system: add .requestMatchers("/api/orders/**").authenticated()
     *   and wire in JWT filter here.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Actuator health endpoints — public (for K8s probes)
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()
                // Swagger UI — public (restrict in prod if needed)
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**").permitAll()
                // H2 console (local only)
                .requestMatchers("/h2-console/**").permitAll()
                // Static frontend assets
                .requestMatchers("/", "/index.html", "/styles.css", "/app.js").permitAll()
                // SSE events endpoint
                .requestMatchers(HttpMethod.GET, "/api/events").permitAll()
                // Order CRUD — open for this demo; add .authenticated() for real system
                .requestMatchers("/api/orders/**").permitAll()
                .anyRequest().authenticated()
            )
            // Allow H2 console to render in iframe (local only)
            .headers(headers -> headers.frameOptions(fo -> fo.sameOrigin()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Parse comma-separated allowed origins from config
        List<String> origins = Arrays.asList(allowedOriginsRaw.split(","));
        config.setAllowedOrigins(origins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-ID"));
        config.setExposedHeaders(List.of("X-Correlation-ID"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
