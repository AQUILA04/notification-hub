package com.optimizesolux.notificationhub.otp.infrastructure;

import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OtpProviderRegistryTest {

    @Test
    void resolvesTwilioVerifyProvider() {
        NotificationHubProperties props = properties("twilio-verify");
        OtpProviderRegistry registry =
                new OtpProviderRegistry(
                        props,
                        java.util.List.of(
                                new TwilioVerifyOtpProvider(
                                        props,
                                        new org.springframework.boot.web.client.RestTemplateBuilder(),
                                        new com.fasterxml.jackson.databind.ObjectMapper()),
                                new InternalOtpProvider(
                                        null, null, props, null)));

        assertEquals("twilio-verify", registry.require().id());
    }

    @Test
    void resolvesInternalProvider() {
        NotificationHubProperties props = properties("internal");
        InternalOtpProvider internal = org.mockito.Mockito.mock(InternalOtpProvider.class);
        org.mockito.Mockito.when(internal.id()).thenReturn("internal");

        OtpProviderRegistry registry =
                new OtpProviderRegistry(props, java.util.List.of(internal));

        assertEquals("internal", registry.require().id());
    }

    @Test
    void rejectsUnknownProvider() {
        NotificationHubProperties props = properties("unknown");
        OtpProviderRegistry registry = new OtpProviderRegistry(props, java.util.List.of());

        assertThrows(IllegalStateException.class, registry::require);
    }

    private static NotificationHubProperties properties(String provider) {
        return new NotificationHubProperties(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new NotificationHubProperties.Otp(
                        true,
                        provider,
                        "VAtest",
                        true,
                        null,
                        null,
                        6,
                        300,
                        5,
                        60,
                        "SMS",
                        "Code {{code}}"));
    }
}
