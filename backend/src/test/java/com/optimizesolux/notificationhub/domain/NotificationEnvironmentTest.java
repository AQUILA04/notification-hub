package com.optimizesolux.notificationhub.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationEnvironmentTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void blankOrUnknownDefaultsToTest() {
        assertEquals(NotificationEnvironment.TEST, NotificationEnvironment.from((String) null));
        assertEquals(NotificationEnvironment.TEST, NotificationEnvironment.from(""));
        assertEquals(NotificationEnvironment.TEST, NotificationEnvironment.from("test"));
        assertEquals(NotificationEnvironment.TEST, NotificationEnvironment.from("local"));
        assertEquals(NotificationEnvironment.TEST, NotificationEnvironment.from("dev"));
        assertEquals(NotificationEnvironment.TEST, NotificationEnvironment.from((NotificationEnvironment) null));
    }

    @Test
    void prodAliases() {
        assertEquals(NotificationEnvironment.PROD, NotificationEnvironment.from("prod"));
        assertEquals(NotificationEnvironment.PROD, NotificationEnvironment.from("PROD"));
        assertEquals(NotificationEnvironment.PROD, NotificationEnvironment.from("production"));
        assertTrue(NotificationEnvironment.PROD.isProd());
        assertFalse(NotificationEnvironment.TEST.isProd());
    }

    @Test
    void jsonRoundTripUsesLowercase() throws Exception {
        assertEquals("\"test\"", mapper.writeValueAsString(NotificationEnvironment.TEST));
        assertEquals("\"prod\"", mapper.writeValueAsString(NotificationEnvironment.PROD));
        assertEquals(NotificationEnvironment.TEST, mapper.readValue("\"test\"", NotificationEnvironment.class));
        assertEquals(NotificationEnvironment.PROD, mapper.readValue("\"prod\"", NotificationEnvironment.class));
        assertEquals(NotificationEnvironment.TEST, mapper.readValue("\"staging\"", NotificationEnvironment.class));
    }
}
