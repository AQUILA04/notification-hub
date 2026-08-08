package com.optimizesolux.notificationhub.infrastructure.provider;

import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EmailProviderRegistry {

    private final Map<String, EmailProvider> providersById;
    private final NotificationHubProperties properties;

    public EmailProviderRegistry(List<EmailProvider> providers, NotificationHubProperties properties) {
        this.properties = properties;
        this.providersById =
                providers.stream()
                        .collect(Collectors.toMap(p -> p.id().toLowerCase(Locale.ROOT), Function.identity()));
        if (providersById.isEmpty()) {
            throw new IllegalStateException("No EmailProvider beans registered");
        }
    }

    public EmailProvider active() {
        String key =
                properties.email() == null || properties.email().provider() == null
                        ? "smtp"
                        : properties.email().provider().toLowerCase(Locale.ROOT);
        EmailProvider provider = providersById.get(key);
        if (provider == null) {
            throw new IllegalStateException(
                    "Unknown email provider '"
                            + key
                            + "'. Registered: "
                            + providersById.keySet());
        }
        return provider;
    }
}
