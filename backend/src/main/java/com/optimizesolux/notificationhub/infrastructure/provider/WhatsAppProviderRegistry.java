package com.optimizesolux.notificationhub.infrastructure.provider;

import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(prefix = "notification-hub.channels", name = "whatsapp", havingValue = "true")
public class WhatsAppProviderRegistry {

    private final Map<String, WhatsAppProvider> providersById;
    private final NotificationHubProperties properties;

    public WhatsAppProviderRegistry(
            List<WhatsAppProvider> providers, NotificationHubProperties properties) {
        this.properties = properties;
        this.providersById =
                providers.stream()
                        .collect(
                                Collectors.toMap(
                                        p -> p.id().toLowerCase(Locale.ROOT), Function.identity()));
        if (providersById.isEmpty()) {
            throw new IllegalStateException(
                    "WhatsApp channel enabled but no WhatsAppProvider bean registered");
        }
    }

    public WhatsAppProvider active() {
        String key =
                properties.whatsapp().provider() == null
                        ? "twilio"
                        : properties.whatsapp().provider().toLowerCase(Locale.ROOT);
        WhatsAppProvider provider = providersById.get(key);
        if (provider == null) {
            throw new IllegalStateException(
                    "Unknown WhatsApp provider '" + key + "'. Registered: " + providersById.keySet());
        }
        return provider;
    }
}
