package com.optimizesolux.notificationhub.otp.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneNormalizerTest {

    @Test
    void normalizesToE164() {
        assertEquals("+22890909090", PhoneNormalizer.normalize("+22890909090"));
        assertEquals("+22890909090", PhoneNormalizer.normalize("22890909090"));
        assertEquals("+22890909090", PhoneNormalizer.normalize("whatsapp:+22890909090"));
    }

    @Test
    void rejectsInvalidNumbers() {
        assertThrows(IllegalArgumentException.class, () -> PhoneNormalizer.normalize(""));
        assertThrows(IllegalArgumentException.class, () -> PhoneNormalizer.normalize("abc"));
    }
}
