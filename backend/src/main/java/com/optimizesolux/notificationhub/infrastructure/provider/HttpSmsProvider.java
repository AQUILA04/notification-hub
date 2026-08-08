package com.optimizesolux.notificationhub.infrastructure.provider;

import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Provider générique : POST JSON {from,to,body} vers une URL configurée. */
@Component
@ConditionalOnProperty(prefix = "notification-hub.sms", name = "provider", havingValue = "http")
public class HttpSmsProvider implements SmsProvider {

    private final NotificationHubProperties properties;
    private final RestTemplate restTemplate;

    public HttpSmsProvider(NotificationHubProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        this.restTemplate =
                builder
                        .setConnectTimeout(Duration.ofSeconds(5))
                        .setReadTimeout(Duration.ofSeconds(15))
                        .build();
    }

    @Override
    public String id() {
        return "http";
    }

    @Override
    public String send(String from, List<String> to, String body) throws Exception {
        NotificationHubProperties.Sms sms = properties.sms();
        if (sms.httpUrl() == null || sms.httpUrl().isBlank()) {
            throw new IllegalStateException("notification-hub.sms.http-url is required for http provider");
        }
        Map<String, Object> payload = Map.of("from", from, "to", to, "body", body);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (sms.httpAuthHeader() != null && !sms.httpAuthHeader().isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, sms.httpAuthHeader());
        }
        ResponseEntity<String> response =
                restTemplate.postForEntity(sms.httpUrl(), new HttpEntity<>(payload, headers), String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("SMS HTTP provider returned " + response.getStatusCode());
        }
        return "sms-http-" + UUID.randomUUID();
    }
}
