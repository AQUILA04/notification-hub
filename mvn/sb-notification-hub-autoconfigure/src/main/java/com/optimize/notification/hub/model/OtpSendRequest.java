package com.optimize.notification.hub.model;

import java.util.Map;

public record OtpSendRequest(
        String to, Channel channel, Map<String, Object> metadata, NotificationEnvironment environment) {

    public OtpSendRequest(String to, Channel channel, Map<String, Object> metadata) {
        this(to, channel, metadata, null);
    }

    public static OtpSendRequest of(String to) {
        return new OtpSendRequest(to, null, null, null);
    }

    public static OtpSendRequest whatsApp(String to) {
        return new OtpSendRequest(to, Channel.WHATSAPP, null, null);
    }

    public static OtpSendRequest sms(String to) {
        return new OtpSendRequest(to, Channel.SMS, null, null);
    }

    public OtpSendRequest withEnvironment(NotificationEnvironment environment) {
        return new OtpSendRequest(to, channel, metadata, environment);
    }
}
