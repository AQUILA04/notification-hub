package com.optimizesolux.notificationhub.otp.api.dto;

import com.optimizesolux.notificationhub.domain.Channel;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record OtpVerifyRequest(
        @NotBlank String to,
        @NotBlank String code,
        Channel channel,
        UUID sessionId) {}
