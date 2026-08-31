package com.optimizesolux.notificationhub.otp.domain;

/** Result of an OTP verification attempt. */
public enum OtpVerifyReason {
    VALID,
    INVALID,
    EXPIRED,
    MAX_ATTEMPTS
}
