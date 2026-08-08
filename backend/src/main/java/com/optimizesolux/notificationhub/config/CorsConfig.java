package com.optimizesolux.notificationhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    private final NotificationHubProperties properties;

    public CorsConfig(NotificationHubProperties properties) {
        this.properties = properties;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = new ArrayList<>(Arrays.asList(properties.cors().allowedOrigins()));
        String fromEnv = System.getenv("CORS_ORIGINS");
        if (fromEnv != null && !fromEnv.isBlank()) {
            for (String part : fromEnv.split(",")) {
                String origin = part.trim();
                if (!origin.isEmpty() && !origins.contains(origin)) {
                    origins.add(origin);
                }
            }
        }
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Idempotency-Key", "X-Tenant-Id", "X-App-Id"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
