package com.optimizesolux.notificationhub.infrastructure.provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AfrikSmsProviderTest {

    @Test
    void normalizeMsisdnStripsPlusAnd00() {
        assertEquals("22890909090", AfrikSmsProvider.normalizeMsisdn("+228 90 90 90 90"));
        assertEquals("22890909090", AfrikSmsProvider.normalizeMsisdn("0022890909090"));
        assertEquals("22890909090", AfrikSmsProvider.normalizeMsisdn("22890909090"));
    }
}
