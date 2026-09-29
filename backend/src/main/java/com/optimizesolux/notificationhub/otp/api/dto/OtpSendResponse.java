package com.optimizesolux.notificationhub.otp.api.dto;

import com.optimizesolux.notificationhub.domain.Channel;

import java.time.Instant;
import java.util.UUID;

public record OtpSendResponse(
        UUID sessionId,
        Instant expiresAt,
        UUID notificationId,
        Channel channel,
        String provider,
        String providerReference,
        /** Référence courte (ex. Y4GP) pour distinguer les SMS OTP — null si non applicable (Twilio Verify). */
        String reference) {

    /** Backward-compatible constructor without provider metadata. */
    public OtpSendResponse(
            UUID sessionId, Instant expiresAt, UUID notificationId, Channel channel) {
        this(sessionId, expiresAt, notificationId, channel, null, null, null);
    }

    /** Backward-compatible constructor without OTP display reference. */
    public OtpSendResponse(
            UUID sessionId,
            Instant expiresAt,
            UUID notificationId,
            Channel channel,
            String provider,
            String providerReference) {
        this(sessionId, expiresAt, notificationId, channel, provider, providerReference, null);
    }
}
