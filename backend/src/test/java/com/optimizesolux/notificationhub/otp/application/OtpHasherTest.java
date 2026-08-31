package com.optimizesolux.notificationhub.otp.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpHasherTest {

    @Test
    void matchesKnownHash() {
        String hash = OtpHasher.hash("tenant-a", "+22890909090", "424242");
        assertTrue(OtpHasher.matches("tenant-a", "+22890909090", "424242", hash));
        assertFalse(OtpHasher.matches("tenant-a", "+22890909090", "000000", hash));
        assertFalse(OtpHasher.matches("tenant-b", "+22890909090", "424242", hash));
    }
}
