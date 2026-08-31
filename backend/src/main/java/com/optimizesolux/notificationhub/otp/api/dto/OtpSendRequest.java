package com.optimizesolux.notificationhub.otp.api.dto;

import com.optimizesolux.notificationhub.domain.Channel;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record OtpSendRequest(
        @NotBlank String to,
        Channel channel,
        Map<String, Object> metadata) {}
