package com.optimizesolux.notificationhub.otp.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Hashes OTP codes for storage (never store plaintext in Redis). */
public final class OtpHasher {

    private OtpHasher() {}

    public static String hash(String tenantId, String destination, String code) {
        String payload = tenantId + "|" + destination + "|" + code;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static boolean matches(String tenantId, String destination, String code, String expectedHash) {
        if (expectedHash == null || expectedHash.isBlank()) {
            return false;
        }
        String actual = hash(tenantId, destination, code);
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8), expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}
