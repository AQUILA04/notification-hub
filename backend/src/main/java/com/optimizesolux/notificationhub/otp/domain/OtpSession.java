package com.optimizesolux.notificationhub.otp.domain;

import com.optimizesolux.notificationhub.domain.Channel;

import java.time.Instant;
import java.util.UUID;

/** OTP session stored in Redis (code hash only, never plaintext). */
public record OtpSession(
        String tenantId,
        UUID sessionId,
        Channel channel,
        String destination,
        String codeHash,
        Instant expiresAt) {}
