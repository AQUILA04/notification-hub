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
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * WhatsApp via Meta Cloud API (Graph).
 *
 * <p>Templates : {@code contentSidOrBody} = template name (pas HX). OTP body param = {@code
 * templateData["1"]}. Session 24h : texte libre si pas de template name reconnu.
 */
@Component
@ConditionalOnProperty(prefix = "notification-hub.channels", name = "whatsapp", havingValue = "true")
public class MetaWhatsAppProvider implements WhatsAppProvider {

    private static final Logger log = LoggerFactory.getLogger(MetaWhatsAppProvider.class);

    private final NotificationHubProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MetaWhatsAppProvider(
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
        return "meta";
    }

    @Override
    public String send(
            String from, List<String> to, String contentSidOrBody, Map<String, Object> templateData)
            throws Exception {
        NotificationHubProperties.Meta meta = requireMeta();
        String token = meta.token();
        String phoneNumberId = meta.phoneNumberId();
        if (token == null || token.isBlank() || phoneNumberId == null || phoneNumberId.isBlank()) {
            throw new IllegalStateException(
                    "Meta WhatsApp requires WHATSAPP_TOKEN and WHATSAPP_PHONE_NUMBER_ID");
        }

        String version =
                meta.graphVersion() != null && !meta.graphVersion().isBlank()
                        ? meta.graphVersion()
                        : "v25.0";
        String url =
                "https://graph.facebook.com/" + version + "/" + phoneNumberId + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        boolean useTemplate =
                contentSidOrBody != null
                        && !contentSidOrBody.isBlank()
                        && !looksLikeFreeText(contentSidOrBody, templateData);

        String lastId = null;
        for (String destination : to) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("to", normalizeRecipient(destination));
            if (useTemplate) {
                payload.put("type", "template");
                payload.put("template", buildTemplate(contentSidOrBody, templateData, meta));
            } else {
                payload.put("type", "text");
                payload.put(
                        "text",
                        Map.of(
                                "body",
                                contentSidOrBody != null ? contentSidOrBody : "",
                                "preview_url",
                                false));
            }

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Meta WhatsApp returned " + response.getStatusCode());
            }
            lastId = extractWamid(response.getBody());
            log.info("WhatsApp Meta sent to={} wamid={}", destination, lastId);
        }
        return lastId != null ? lastId : "wamid.meta-" + UUID.randomUUID();
    }

    private Map<String, Object> buildTemplate(
            String templateName, Map<String, Object> templateData, NotificationHubProperties.Meta meta) {
        String lang =
                meta.otpTemplateLang() != null && !meta.otpTemplateLang().isBlank()
                        ? meta.otpTemplateLang()
                        : "fr";
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", templateName);
        template.put("language", Map.of("code", lang));

        List<Map<String, Object>> components = new ArrayList<>();
        String otpCode = firstTemplateParam(templateData);
        if (otpCode != null) {
            components.add(
                    Map.of(
                            "type",
                            "body",
                            "parameters",
                            List.of(Map.of("type", "text", "text", otpCode))));
            String button = meta.otpButton();
            if (button == null || button.isBlank() || "copy_code".equalsIgnoreCase(button)) {
                components.add(
                        Map.of(
                                "type",
                                "button",
                                "sub_type",
                                "url",
                                "index",
                                "0",
                                "parameters",
                                List.of(Map.of("type", "text", "text", otpCode))));
            }
        } else if (templateData != null && !templateData.isEmpty()) {
            List<Map<String, Object>> params = new ArrayList<>();
            templateData.values().stream()
                    .map(v -> Map.<String, Object>of("type", "text", "text", String.valueOf(v)))
                    .forEach(params::add);
            components.add(Map.of("type", "body", "parameters", params));
        }
        if (!components.isEmpty()) {
            template.put("components", components);
        }
        return template;
    }

    private static String firstTemplateParam(Map<String, Object> templateData) {
        if (templateData == null || templateData.isEmpty()) {
            return null;
        }
        Object v = templateData.get("1");
        if (v == null) {
            v = templateData.values().iterator().next();
        }
        return v != null ? String.valueOf(v) : null;
    }

    /**
     * Free text if no templateData and body looks like a sentence; otherwise treat as template name.
     */
    private static boolean looksLikeFreeText(String body, Map<String, Object> templateData) {
        if (templateData != null && !templateData.isEmpty()) {
            return false;
        }
        // Meta template names: lowercase alnum + underscore, no spaces
        return body.contains(" ") || body.contains("\n");
    }

    static String normalizeRecipient(String raw) {
        if (raw == null) {
            return "";
        }
        String v = raw.trim();
        if (v.startsWith("whatsapp:")) {
            v = v.substring("whatsapp:".length()).trim();
        }
        if (v.startsWith("+")) {
            v = v.substring(1);
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    private String extractWamid(Map<?, ?> body) {
        Object messages = body.get("messages");
        if (messages instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> m) {
                Object id = m.get("id");
                if (id != null) {
                    return id.toString();
                }
            }
        }
        try {
            JsonNode node = objectMapper.valueToTree(body);
            JsonNode id = node.path("messages").path(0).path("id");
            if (!id.isMissingNode() && !id.asText().isBlank()) {
                return id.asText();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "wamid.meta-" + UUID.randomUUID();
    }

    private NotificationHubProperties.Meta requireMeta() {
        NotificationHubProperties.Whatsapp wa = properties.whatsapp();
        if (wa == null || wa.meta() == null) {
            throw new IllegalStateException("notification-hub.whatsapp.meta is not configured");
        }
        return wa.meta();
    }
}
