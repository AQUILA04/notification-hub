package com.optimizesolux.notificationhub.otp.application;

import java.security.SecureRandom;

/**
 * Génère une référence alphanumérique courte associée à un OTP (affichage UX + SMS).
 * Alphabet sans caractères ambigus (0/O, 1/I/L).
 */
public final class OtpReferenceGenerator {

    /** Alphabet sans 0/O, 1/I/L pour limiter les confusions à l'affichage. */
    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private static final SecureRandom RANDOM = new SecureRandom();

    private OtpReferenceGenerator() {}

    /** Génère une référence de longueur donnée (clampée 4–8). */
    public static String generate(int length) {
        int safeLength = Math.max(4, Math.min(length, 8));
        StringBuilder sb = new StringBuilder(safeLength);
        for (int i = 0; i < safeLength; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
