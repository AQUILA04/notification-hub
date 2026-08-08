package com.optimizesolux.notificationhub.infrastructure.provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TwilioWhatsAppProviderTest {

    @Test
    void normalizeAddsWhatsappPrefix() {
        assertEquals("whatsapp:+22890000000", TwilioWhatsAppProvider.normalizeWhatsAppAddress("+22890000000"));
        assertEquals("whatsapp:+22890000000", TwilioWhatsAppProvider.normalizeWhatsAppAddress("whatsapp:+22890000000"));
        assertEquals("whatsapp:+22890000000", TwilioWhatsAppProvider.normalizeWhatsAppAddress("22890000000"));
    }
}
