package com.optimizesolux.notificationhub.api;

/**
 * WhatsApp OTP requested but hub Twilio template/sender configuration is incomplete.
 */
public class OtpConfigurationException extends RuntimeException {

    public OtpConfigurationException(String message) {
        super(message);
    }
}
