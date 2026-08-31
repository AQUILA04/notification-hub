package com.optimizesolux.notificationhub.otp.application;

import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import com.optimizesolux.notificationhub.domain.Channel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpCodeGeneratorTest {

    @Test
    void generatesNumericCodeOfRequestedLength() {
        String code = OtpCodeGenerator.generate(6);
        assertEquals(6, code.length());
        assertTrue(code.chars().allMatch(Character::isDigit));
    }

    @Test
    void clampsLengthToSupportedRange() {
        assertEquals(4, OtpCodeGenerator.generate(2).length());
        assertEquals(8, OtpCodeGenerator.generate(20).length());
    }
}
