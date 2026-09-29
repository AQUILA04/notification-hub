package com.optimizesolux.notificationhub.otp.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpReferenceGeneratorTest {

    @Test
    void generatesAlphanumericReferenceOfRequestedLength() {
        String ref = OtpReferenceGenerator.generate(4);
        assertEquals(4, ref.length());
        assertTrue(ref.chars().allMatch(c -> OtpReferenceGenerator.ALPHABET.indexOf(c) >= 0));
    }

    @Test
    void clampsLengthToSupportedRange() {
        assertEquals(4, OtpReferenceGenerator.generate(2).length());
        assertEquals(8, OtpReferenceGenerator.generate(20).length());
    }

    @Test
    void excludesAmbiguousCharacters() {
        for (int i = 0; i < 200; i++) {
            String ref = OtpReferenceGenerator.generate(8);
            assertTrue(ref.indexOf('0') < 0);
            assertTrue(ref.indexOf('O') < 0);
            assertTrue(ref.indexOf('1') < 0);
            assertTrue(ref.indexOf('I') < 0);
            assertTrue(ref.indexOf('L') < 0);
        }
    }
}
