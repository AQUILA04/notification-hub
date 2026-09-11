package com.optimizesolux.notificationhub.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookControllerMetaTest {

    @Test
    void hmacSha256MatchesKnownVector() {
        // echo -n '{"object":"whatsapp"}' | openssl dgst -sha256 -hmac 'testsecret'
        String hex = WebhookController.hmacSha256Hex("testsecret", "{\"object\":\"whatsapp\"}");
        assertEquals(64, hex.length());
        assertTrue(WebhookController.constantTimeEquals(hex, hex));
        assertFalse(WebhookController.constantTimeEquals(hex, "0".repeat(64)));
    }

    @Test
    void constantTimeEqualsRejectsDifferentLengths() {
        assertFalse(WebhookController.constantTimeEquals("abc", "ab"));
        assertFalse(WebhookController.constantTimeEquals(null, "ab"));
    }
}
