package com.optimizesolux.notificationhub.otp.application;

import java.security.SecureRandom;

public final class OtpCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private OtpCodeGenerator() {}

    /** Generates a numeric OTP of the given length (4–8). */
    public static String generate(int length) {
        int safeLength = Math.max(4, Math.min(length, 8));
        int bound = (int) Math.pow(10, safeLength);
        int min = bound / 10;
        int value = RANDOM.nextInt(bound - min) + min;
        return String.valueOf(value);
    }
}
