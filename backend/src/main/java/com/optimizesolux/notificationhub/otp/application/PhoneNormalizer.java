package com.optimizesolux.notificationhub.otp.application;

/** Normalises phone numbers to E.164 (+…). */
public final class PhoneNormalizer {

    private PhoneNormalizer() {}

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        String v = raw.trim();
        if (v.startsWith("whatsapp:")) {
            v = v.substring("whatsapp:".length());
        }
        if (!v.startsWith("+") && v.chars().allMatch(Character::isDigit)) {
            v = "+" + v;
        }
        if (!v.startsWith("+") || v.length() < 8) {
            throw new IllegalArgumentException("Phone number must be E.164 format (e.g. +22890909090)");
        }
        String digits = v.substring(1);
        if (!digits.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Phone number must be E.164 format (e.g. +22890909090)");
        }
        return v;
    }
}
