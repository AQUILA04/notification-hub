package com.optimize.notification.hub.model;

import java.util.UUID;

public record OtpVerifyRequest(String to, String code, Channel channel, UUID sessionId) {

    public static OtpVerifyRequest of(String to, String code) {
        return new OtpVerifyRequest(to, code, null, null);
    }
}
