package com.optimizesolux.notificationhub.otp.api.dto;

import com.optimizesolux.notificationhub.domain.Channel;

import java.time.Instant;
import java.util.UUID;

public record OtpSendResponse(
        UUID sessionId,
        Instant expiresAt,
        UUID notificationId,
        Channel channel) {}
