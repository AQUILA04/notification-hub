package com.optimize.notification.hub.model;

import java.util.Map;

public record OtpSendRequest(String to, Channel channel, Map<String, Object> metadata) {

    public static OtpSendRequest of(String to) {
        return new OtpSendRequest(to, null, null);
    }

    public static OtpSendRequest whatsApp(String to) {
        return new OtpSendRequest(to, Channel.WHATSAPP, null);
    }

    public static OtpSendRequest sms(String to) {
        return new OtpSendRequest(to, Channel.SMS, null);
    }
}
