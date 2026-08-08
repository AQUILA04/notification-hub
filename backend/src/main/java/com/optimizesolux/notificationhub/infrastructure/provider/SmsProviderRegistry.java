package com.optimizesolux.notificationhub.infrastructure.provider;

import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sélectionne le {@link SmsProvider} actif via {@code notification-hub.sms.provider}.
 * Ajouter AfrikSMS = nouvelle classe {@link SmsProvider} + éventuellement
 * {@code @ConditionalOnProperty(... havingValue = "afriksms")} ; puis
 * {@code SMS_PROVIDER=afriksms} — zéro changement ici si le bean est toujours présent,
 * ou un seul case de config.
 */
@Component
public class SmsProviderRegistry {

    private final Map<String, SmsProvider> providersById;
    private final NotificationHubProperties properties;

    public SmsProviderRegistry(List<SmsProvider> providers, NotificationHubProperties properties) {
        this.properties = properties;
        this.providersById =
                providers.stream()
                        .collect(Collectors.toMap(p -> p.id().toLowerCase(Locale.ROOT), Function.identity()));
        if (providersById.isEmpty()) {
            throw new IllegalStateException("No SmsProvider beans registered");
        }
    }

    public SmsProvider active() {
        String key =
                properties.sms().provider() == null
                        ? "logging"
                        : properties.sms().provider().toLowerCase(Locale.ROOT);
        SmsProvider provider = providersById.get(key);
        if (provider == null) {
            throw new IllegalStateException(
                    "Unknown SMS provider '"
                            + key
                            + "'. Registered: "
                            + providersById.keySet()
                            + ". Implement SmsProvider and set notification-hub.sms.provider.");
        }
        return provider;
    }
}
