package com.optimizesolux.notificationhub.otp.infrastructure;

import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class OtpProviderRegistry {

    private final NotificationHubProperties properties;
    private final List<OtpProvider> providers;

    public OtpProviderRegistry(NotificationHubProperties properties, List<OtpProvider> providers) {
        this.properties = properties;
        this.providers = providers;
    }

    public OtpProvider require() {
        String configured = properties.otp().provider();
        String id = configured != null ? configured.trim().toLowerCase(Locale.ROOT) : "internal";
        return providers.stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Unknown OTP provider: "
                                                + id
                                                + " (available: internal, twilio-verify)"));
    }
}
