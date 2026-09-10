package com.optimizesolux.notificationhub.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Client-declared delivery environment for paid channels (SMS).
 *
 * <p>{@code prod} sends a real SMS. Any other value (including omitted) is treated as
 * {@code test}: the payload is delivered to Mailpit via SMTP instead of consuming SMS
 * credits.
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

    public static NotificationEnvironment from(NotificationEnvironment value) {
        return value == null ? TEST : value;
    }

    public boolean isProd() {
        return this == PROD;
    }
}
