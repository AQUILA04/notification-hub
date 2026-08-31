package com.optimizesolux.notificationhub.otp.api.dto;

import com.optimizesolux.notificationhub.otp.domain.OtpVerifyReason;

public record OtpVerifyResponse(boolean valid, OtpVerifyReason reason) {

    public static OtpVerifyResponse success() {
        return new OtpVerifyResponse(true, OtpVerifyReason.VALID);
    }

    public static OtpVerifyResponse failure(OtpVerifyReason reason) {
        return new OtpVerifyResponse(false, reason);
    }
}
