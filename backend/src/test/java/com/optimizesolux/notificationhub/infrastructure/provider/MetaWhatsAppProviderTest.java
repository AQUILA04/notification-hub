package com.optimizesolux.notificationhub.infrastructure.provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetaWhatsAppProviderTest {

    @Test
    void normalizeRecipientStripsPlusAndWhatsappPrefix() {
        assertEquals("22892181351", MetaWhatsAppProvider.normalizeRecipient("+22892181351"));
        assertEquals("22892181351", MetaWhatsAppProvider.normalizeRecipient("whatsapp:+22892181351"));
        assertEquals("22892181351", MetaWhatsAppProvider.normalizeRecipient("22892181351"));
    }
}
