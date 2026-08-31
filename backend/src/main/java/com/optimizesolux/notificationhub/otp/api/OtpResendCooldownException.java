package com.optimizesolux.notificationhub.otp.api;

/**
 * Thrown when a new OTP is requested before the resend cooldown expires.
 */
public class OtpResendCooldownException extends RuntimeException {

    private final long retryAfterSeconds;

    public OtpResendCooldownException(long retryAfterSeconds) {
        super("OTP resend cooldown active — retry in " + retryAfterSeconds + " seconds");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
