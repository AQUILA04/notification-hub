package com.optimizesolux.notificationhub.otp.api.dto;

import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.NotificationEnvironment;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record OtpSendRequest(
        @NotBlank String to,
        Channel channel,
        Map<String, Object> metadata,
        NotificationEnvironment environment) {

    public OtpSendRequest {
        environment = NotificationEnvironment.from(environment);
    }

    /** Backward-compatible constructor without environment. */
    public OtpSendRequest(String to, Channel channel, Map<String, Object> metadata) {
        this(to, channel, metadata, null);
    }
}
