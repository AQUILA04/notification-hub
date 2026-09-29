package com.optimize.notification.hub.model;

import java.time.Instant;
import java.util.UUID;

public record OtpSendResponse(
        UUID sessionId,
        Instant expiresAt,
        UUID notificationId,
        Channel channel,
        String provider,
        String providerReference,
        /** Référence courte affichable (ex. Y4GP) — null si le provider ne la fournit pas. */
        String reference) {}
