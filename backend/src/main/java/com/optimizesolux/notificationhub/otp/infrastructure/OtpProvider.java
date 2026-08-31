package com.optimizesolux.notificationhub.otp.infrastructure;

import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.otp.api.dto.OtpSendRequest;
import com.optimizesolux.notificationhub.otp.api.dto.OtpSendResponse;
import com.optimizesolux.notificationhub.otp.api.dto.OtpVerifyRequest;
import com.optimizesolux.notificationhub.otp.api.dto.OtpVerifyResponse;

public interface OtpProvider {

    /** @return provider id, e.g. internal or twilio-verify */
    String id();

    OtpSendResponse send(OtpSendRequest request, String idempotencyKey, String appIdHeader);

    OtpVerifyResponse verify(OtpVerifyRequest request);

    /** Maps hub channel to provider-specific channel name (for logging). */
    default String describeChannel(Channel channel) {
        return channel.name();
    }
}
