package com.optimizesolux.notificationhub.api;

import com.optimizesolux.notificationhub.application.ProviderWebhookService;
import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/v1/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final ProviderWebhookService providerWebhookService;
    private final NotificationHubProperties properties;

    public WebhookController(
            ProviderWebhookService providerWebhookService, NotificationHubProperties properties) {
        this.providerWebhookService = providerWebhookService;
        this.properties = properties;
    }

    /** Twilio StatusCallback (application/x-www-form-urlencoded). */
    @PostMapping("/twilio")
    public ResponseEntity<Void> twilioStatus(@RequestParam MultiValueMap<String, String> form) {
        providerWebhookService.handleTwilioStatus(
                form.getFirst("MessageSid"),
                form.getFirst("MessageStatus"),
                form.getFirst("ErrorMessage"));
        return ResponseEntity.noContent().build();
    }

    /** Brevo transactional SMS webUrl callback (JSON). */
    @PostMapping("/brevo")
    public ResponseEntity<Void> brevoSmsStatus(@RequestBody Map<String, Object> payload) {
        Object messageId = payload.get("messageId");
        Object msgStatus = payload.get("msg_status");
        Object description = payload.get("description");
        providerWebhookService.handleBrevoSmsStatus(
                messageId != null ? String.valueOf(messageId) : null,
                msgStatus != null ? String.valueOf(msgStatus) : null,
                description != null ? String.valueOf(description) : null);
        return ResponseEntity.noContent().build();
    }

    /**
     * Meta Cloud API webhook verification handshake.
     *
     * @see <a href="https://developers.facebook.com/docs/graph-api/webhooks/getting-started">Meta
     *     webhooks</a>
     */
    @GetMapping(value = "/meta", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> metaVerify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        String expected = metaVerifyToken();
        if (!"subscribe".equals(mode)
                || expected == null
                || expected.isBlank()
                || token == null
                || !expected.equals(token)
                || challenge == null
                || challenge.isBlank()) {
            log.warn("Meta webhook verify rejected mode={} tokenMatch={}", mode, expected != null && expected.equals(token));
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(challenge);
    }

    /**
     * Meta Cloud API delivery / inbound webhook. Signature verified with App Secret when
     * configured.
     */
    @PostMapping("/meta")
    public ResponseEntity<Void> metaEvent(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawBody) {
        if (!verifyMetaSignature(signature, rawBody)) {
            log.warn("Meta webhook invalid or missing X-Hub-Signature-256");
            return ResponseEntity.status(403).build();
        }
        providerWebhookService.handleMetaWebhook(rawBody);
        return ResponseEntity.ok().build();
    }

    private String metaVerifyToken() {
        NotificationHubProperties.Meta meta =
                properties.whatsapp() != null ? properties.whatsapp().meta() : null;
        return meta != null ? meta.webhookVerifyToken() : null;
    }

    private String metaAppSecret() {
        NotificationHubProperties.Meta meta =
                properties.whatsapp() != null ? properties.whatsapp().meta() : null;
        return meta != null ? meta.appSecret() : null;
    }

    boolean verifyMetaSignature(String signatureHeader, String rawBody) {
        String appSecret = metaAppSecret();
        if (appSecret == null || appSecret.isBlank()) {
            // Dev / handshake-only: allow if secret not configured (log once per call at warn)
            log.warn("WHATSAPP_APP_SECRET not set — accepting Meta POST without HMAC check");
            return true;
        }
        if (signatureHeader == null || signatureHeader.isBlank() || rawBody == null) {
            return false;
        }
        String expected = "sha256=" + hmacSha256Hex(appSecret, rawBody);
        return constantTimeEquals(
                expected.toLowerCase(Locale.ROOT), signatureHeader.trim().toLowerCase(Locale.ROOT));
    }

    static String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 failed", e);
        }
    }

    static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
