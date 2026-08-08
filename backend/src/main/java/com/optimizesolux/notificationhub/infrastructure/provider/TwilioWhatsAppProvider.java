package com.optimizesolux.notificationhub.infrastructure.provider;

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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * WhatsApp via Twilio Messages API.
 * Templates hors session 24h : ContentSid (HX…) dans templateName ou body.
 */
@Component
@ConditionalOnProperty(prefix = "notification-hub.channels", name = "whatsapp", havingValue = "true")
public class TwilioWhatsAppProvider implements WhatsAppProvider {

    private static final Logger log = LoggerFactory.getLogger(TwilioWhatsAppProvider.class);

    private final NotificationHubProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public TwilioWhatsAppProvider(
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
        return "twilio";
    }

    @Override
    public String send(
            String from, List<String> to, String contentSidOrBody, Map<String, Object> templateData)
            throws Exception {
        String sid = firstNonBlank(properties.whatsapp().accountSid(), properties.sms().accountSid());
        String token = firstNonBlank(properties.whatsapp().authToken(), properties.sms().authToken());
        if (sid == null || token == null || sid.isBlank() || token.isBlank()) {
            throw new IllegalStateException(
                    "Twilio WhatsApp requires TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN");
        }

        String url = "https://api.twilio.com/2010-04-01/Accounts/" + sid + "/Messages.json";
        String auth =
                Base64.getEncoder()
                        .encodeToString((sid + ":" + token).getBytes(StandardCharsets.UTF_8));

        String lastSid = null;
        for (String destination : to) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + auth);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("From", normalizeWhatsAppAddress(from));
            form.add("To", normalizeWhatsAppAddress(destination));

            if (contentSidOrBody != null && contentSidOrBody.startsWith("HX")) {
                form.add("ContentSid", contentSidOrBody);
                if (templateData != null && !templateData.isEmpty()) {
                    form.add("ContentVariables", objectMapper.writeValueAsString(toStringMap(templateData)));
                }
            } else {
                form.add("Body", contentSidOrBody != null ? contentSidOrBody : "");
            }

            String callback = properties.whatsapp().statusCallbackUrl();
            if (callback != null && !callback.isBlank()) {
                form.add("StatusCallback", callback);
            }

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(url, new HttpEntity<>(form, headers), Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Twilio WhatsApp returned " + response.getStatusCode());
            }
            Object msgSid = response.getBody().get("sid");
            lastSid = msgSid != null ? msgSid.toString() : "wa-twilio-" + UUID.randomUUID();
            log.info("WhatsApp Twilio sent to={} sid={}", destination, lastSid);
        }
        return lastSid != null ? lastSid : "wa-twilio-" + UUID.randomUUID();
    }

    static String normalizeWhatsAppAddress(String raw) {
        if (raw == null) {
            return "";
        }
        String v = raw.trim();
        if (v.startsWith("whatsapp:")) {
            return v;
        }
        if (!v.startsWith("+") && !v.isEmpty() && v.chars().allMatch(Character::isDigit)) {
            v = "+" + v;
        }
        return "whatsapp:" + v;
    }

    private static Map<String, String> toStringMap(Map<String, Object> data) {
        return data.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
}
