package com.optimizesolux.notificationhub.api;

import com.optimizesolux.notificationhub.application.ProviderWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/webhooks")
public class WebhookController {

    private final ProviderWebhookService providerWebhookService;

    public WebhookController(ProviderWebhookService providerWebhookService) {
        this.providerWebhookService = providerWebhookService;
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
}
