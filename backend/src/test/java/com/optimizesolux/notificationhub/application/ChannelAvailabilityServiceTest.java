package com.optimizesolux.notificationhub.application;

import com.optimizesolux.notificationhub.api.ChannelNotEnabledException;
import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import com.optimizesolux.notificationhub.domain.Channel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelAvailabilityServiceTest {

    @Test
    void whatsappDisabledByDefaultRejectsWithFrenchMessage() {
        NotificationHubProperties props =
                new NotificationHubProperties(
                        null,
                        null,
                        null,
                        null,
                        new NotificationHubProperties.Channels(true, true, false),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        ChannelAvailabilityService service = new ChannelAvailabilityService(props);

        assertTrue(service.isEnabled(Channel.EMAIL));
        assertTrue(service.isEnabled(Channel.SMS));
        assertFalse(service.isEnabled(Channel.WHATSAPP));

        ChannelNotEnabledException ex =
                assertThrows(
                        ChannelNotEnabledException.class,
                        () -> service.requireEnabled(Channel.WHATSAPP));
        assertTrue(ex.getMessage().contains("WHATSAPP"));
        assertTrue(ex.getMessage().contains("n'est pas activé"));
        assertTrue(ex.getMessage().contains("pour le moment"));
    }
}
