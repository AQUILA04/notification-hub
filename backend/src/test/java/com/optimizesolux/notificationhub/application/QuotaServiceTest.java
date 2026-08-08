package com.optimizesolux.notificationhub.application;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuotaServiceTest {

    @Test
    void resolveAppIdPrefersHeaderThenMetadata() {
        assertEquals("billing", QuotaService.resolveAppId(Map.of("appId", "ignored"), "billing"));
        assertEquals("ctp", QuotaService.resolveAppId(Map.of("appId", "ctp"), null));
        assertEquals("portal", QuotaService.resolveAppId(Map.of("app", "portal"), "  "));
        assertEquals("default", QuotaService.resolveAppId(null, null));
    }
}
