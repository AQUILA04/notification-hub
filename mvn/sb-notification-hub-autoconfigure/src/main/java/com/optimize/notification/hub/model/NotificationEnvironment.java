package com.optimize.notification.hub.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Delivery environment for paid channels (SMS).
 *
 * <p>{@code prod} sends a real SMS. Any other value is {@code test}: the hub delivers the
 * payload is emailed to {@code sms@optimizesolux.com} instead of consuming SMS credits.
 */
public enum NotificationEnvironment {
    TEST,
    PROD;

    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static NotificationEnvironment from(String value) {
        if (value == null || value.isBlank()) {
            return TEST;
        }
        String normalized = value.trim();
        if (normalized.equalsIgnoreCase("prod") || normalized.equalsIgnoreCase("production")) {
            return PROD;
        }
        return TEST;
    }

    public boolean isProd() {
        return this == PROD;
    }
}
