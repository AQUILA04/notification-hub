package com.optimizesolux.notificationhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final NotificationHubProperties properties;
    private final TenantFilter tenantFilter;

    public SecurityConfig(NotificationHubProperties properties, TenantFilter tenantFilter) {
        this.properties = properties;
        this.tenantFilter = tenantFilter;
    }

    @Bean
    @Order(1)
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info")
                            .permitAll();
                    auth.requestMatchers("/v1/webhooks/**").permitAll();
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    if (properties.security().permitLocal()) {
                        auth.anyRequest().permitAll();
                    } else {
                        auth.anyRequest().authenticated();
                    }
                })
                .cors(Customizer.withDefaults())
                .addFilterAfter(tenantFilter, UsernamePasswordAuthenticationFilter.class);

        if (!properties.security().permitLocal()) {
            http.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        }

        return http.build();
    }
}
