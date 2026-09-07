package com.optimizesolux.notificationhub.infrastructure.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Provider SMS Brevo — API transactionnelle ({@code POST /v3/transactionalSMS/send}).
 * Doc : https://developers.brevo.com/docs/transactional-sms-endpoints
 */
@Component
@ConditionalOnProperty(prefix = "notification-hub.sms", name = "provider", havingValue = "brevo")
public class BrevoSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(BrevoSmsProvider.class);
    private static final String DEFAULT_BASE = "https://api.brevo.com/v3";

    private final NotificationHubProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public BrevoSmsProvider(
            NotificationHubProperties properties,
            RestTemplateBuilder builder,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate =
                builder
                        .setConnectTimeout(Duration.ofSeconds(5))
                        .setReadTimeout(Duration.ofSeconds(20))
                        .build();
    }

    @Override
    public String id() {
        return "brevo";
    }

    @Override
    public String send(String from, List<String> to, String body) throws Exception {
        NotificationHubProperties.BrevoSms cfg = properties.sms().brevo();
        if (cfg == null || cfg.apiKey() == null || cfg.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "Brevo SMS requires notification-hub.sms.brevo.api-key (BREVO_API_KEY)");
        }

        String sender = resolveSender(from);
        String content = body != null ? body : "";
        String base =
                cfg.baseUrl() == null || cfg.baseUrl().isBlank()
                        ? DEFAULT_BASE
                        : cfg.baseUrl().replaceAll("/$", "");
        String url = base + "/transactionalSMS/send";

        String lastId = null;
        for (String destination : to) {
            String recipient = normalizeMsisdn(destination);
            if (recipient.isBlank()) {
                throw new IllegalArgumentException("Invalid Brevo SMS recipient: " + destination);
            }
            lastId = sendOne(url, cfg, sender, recipient, content);
        }
        return lastId != null ? lastId : "brevo-" + UUID.randomUUID();
    }

    private String sendOne(
            String url,
            NotificationHubProperties.BrevoSms cfg,
            String sender,
            String recipient,
            String content)
            throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("api-key", cfg.apiKey());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sender", sender);
        payload.put("recipient", recipient);
        payload.put("content", content);
        payload.put("type", "transactional");
        if (cfg.webhookUrl() != null && !cfg.webhookUrl().isBlank()) {
            payload.put("webUrl", cfg.webhookUrl());
        }

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
            return parseSuccess(response, recipient);
        } catch (HttpStatusCodeException ex) {
            String errBody = ex.getResponseBodyAsString();
            throw new IllegalStateException(
                    "Brevo SMS HTTP "
                            + ex.getStatusCode().value()
                            + (errBody == null || errBody.isBlank() ? "" : ": " + errBody),
                    ex);
        }
    }

    private String parseSuccess(ResponseEntity<String> response, String recipient) throws Exception {
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Brevo SMS HTTP " + response.getStatusCode());
        }
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode messageId = root.path("messageId");
        String providerId =
                messageId.isMissingNode() || messageId.isNull()
                        ? "brevo-" + UUID.randomUUID()
                        : "brevo-" + messageId.asText();
        log.info("Brevo SMS sent to={} providerId={}", recipient, providerId);
        return providerId;
    }

    private String resolveSender(String from) {
        if (from != null && !from.isBlank()) {
            String cleaned = from.replaceAll("[^A-Za-z0-9]", "");
            if (cleaned.length() > 11) {
                cleaned = cleaned.substring(0, 11);
            }
            if (!cleaned.isBlank()) {
                return cleaned;
            }
        }
        String def = properties.sms().defaultFrom();
        if (def != null && !def.isBlank()) {
            String cleaned = def.replaceAll("[^A-Za-z0-9]", "");
            if (cleaned.length() > 11) {
                cleaned = cleaned.substring(0, 11);
            }
            if (!cleaned.isBlank()) {
                return cleaned;
            }
        }
        throw new IllegalArgumentException("Brevo sender required (from or sms.default-from, ≤11 alphanum)");
    }

    /** E.164 / local → digits with country code, no + or 00 (Brevo accepts optional +). */
    static String normalizeMsisdn(String raw) {
        if (raw == null) {
            return "";
        }
        String digits = raw.trim().replaceAll("[\\s-]", "");
        if (digits.startsWith("+")) {
            digits = digits.substring(1);
        }
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        }
        return digits.replaceAll("\\D", "");
    }
}
