package com.optimizesolux.notificationhub.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.NotificationEnvironment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateNotificationRequestEnvironmentTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void missingEnvironmentDefaultsToTest() throws Exception {
        CreateNotificationRequest request =
                mapper.readValue(
                        """
                        {
                          "channel": "SMS",
                          "from": "OptimizeSLX",
                          "to": ["+22890909090"],
                          "body": "hello"
                        }
                        """,
                        CreateNotificationRequest.class);
        assertEquals(Channel.SMS, request.channel());
        assertEquals(NotificationEnvironment.TEST, request.environment());
    }

    @Test
    void prodIsAcceptedCaseInsensitive() throws Exception {
        CreateNotificationRequest request =
                mapper.readValue(
                        """
                        {
                          "channel": "SMS",
                          "environment": "PROD",
                          "from": "OptimizeSLX",
                          "to": ["+22890909090"],
                          "body": "hello"
                        }
                        """,
                        CreateNotificationRequest.class);
        assertEquals(NotificationEnvironment.PROD, request.environment());
    }
}
