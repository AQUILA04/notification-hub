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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Provider SMS AfrikSMS — choix prod recommandé (coût).
 * Doc : https://afriksms.com/docapi
 */
@Component
@ConditionalOnProperty(prefix = "notification-hub.sms", name = "provider", havingValue = "afriksms")
public class AfrikSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(AfrikSmsProvider.class);
    private static final int SUCCESS_CODE = 100;

    private final NotificationHubProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AfrikSmsProvider(
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
        return "afriksms";
    }

    @Override
    public String send(String from, List<String> to, String body) throws Exception {
        NotificationHubProperties.AfrikSms cfg = properties.sms().afriksms();
        if (cfg == null
                || cfg.clientId() == null
                || cfg.apiKey() == null
                || cfg.clientId().isBlank()
                || cfg.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "AfrikSMS requires notification-hub.sms.afriksms.client-id and api-key");
        }

        String senderId = resolveSender(from);
        String base =
                cfg.baseUrl() == null || cfg.baseUrl().isBlank()
                        ? "https://api.afriksms.com/api/web/web_v1/outbounds"
                        : cfg.baseUrl().replaceAll("/$", "");

        List<String> mobiles =
                to.stream().map(AfrikSmsProvider::normalizeMsisdn).filter(s -> !s.isBlank()).toList();
        if (mobiles.isEmpty()) {
            throw new IllegalArgumentException("No valid MobileNumbers for AfrikSMS");
        }

        if (mobiles.size() == 1) {
            return sendOne(base, cfg, senderId, body, mobiles.getFirst());
        }
        return sendMulti(base, cfg, senderId, body, mobiles);
    }

    private String sendOne(
            String base,
            NotificationHubProperties.AfrikSms cfg,
            String senderId,
            String body,
            String mobile)
            throws Exception {
        String url =
                UriComponentsBuilder.fromUriString(base + "/send")
                        .queryParam("ClientId", cfg.clientId())
                        .queryParam("ApiKey", cfg.apiKey())
                        .queryParam("SenderId", senderId)
                        .queryParam("Message", body != null ? body : "")
                        .queryParam("MobileNumbers", mobile)
                        .build(true)
                        .toUriString();

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return parseSuccess(response, mobile);
    }

    private String sendMulti(
            String base,
            NotificationHubProperties.AfrikSms cfg,
            String senderId,
            String body,
            List<String> mobiles)
            throws Exception {
        String url = base + "/send_multisms";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("ClientId", cfg.clientId());
        form.add("ApiKey", cfg.apiKey());
        form.add("SenderId", senderId);
        form.add("Message", body != null ? body : "");
        form.add("MobileNumbers", mobiles.stream().collect(Collectors.joining(",")));

        ResponseEntity<String> response =
                restTemplate.postForEntity(url, new HttpEntity<>(form, headers), String.class);
        return parseSuccess(response, mobiles.getFirst());
    }

    private String parseSuccess(ResponseEntity<String> response, String sampleMobile) throws Exception {
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("AfrikSMS HTTP " + response.getStatusCode());
        }
        JsonNode root = objectMapper.readTree(response.getBody());
        int code = root.path("code").asInt(-1);
        if (code != SUCCESS_CODE) {
            String message = root.path("message").asText(response.getBody());
            throw new IllegalStateException("AfrikSMS rejected send code=" + code + " message=" + message);
        }
        String providerId =
                root.path("data").isMissingNode() || root.path("data").isNull()
                        ? "afriksms-" + UUID.randomUUID()
                        : "afriksms-" + root.path("data").asText(UUID.randomUUID().toString());
        log.info("AfrikSMS sent sampleTo={} providerId={} code={}", sampleMobile, providerId, code);
        return providerId;
    }

    private String resolveSender(String from) {
        if (from != null && !from.isBlank()) {
            // AfrikSMS SenderId: alphanumeric max 11 chars
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
            return def.length() > 11 ? def.substring(0, 11) : def;
        }
        throw new IllegalArgumentException("AfrikSMS SenderId required (from or sms.default-from)");
    }

    /** E.164 / local → digits with country code, no + or 00. */
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
