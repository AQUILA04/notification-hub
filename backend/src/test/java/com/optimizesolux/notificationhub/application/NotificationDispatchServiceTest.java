package com.optimizesolux.notificationhub.application;

import com.optimizesolux.notificationhub.domain.Priority;
import com.optimizesolux.notificationhub.infrastructure.messaging.OutboxPublisher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationDispatchServiceTest {

    @Test
    void computeDelayUsesExponentialBackoffCapped() {
        assertEquals(2000, NotificationDispatchService.computeDelayMs(1, 2000, 2.0, 300_000));
        assertEquals(4000, NotificationDispatchService.computeDelayMs(2, 2000, 2.0, 300_000));
        assertEquals(8000, NotificationDispatchService.computeDelayMs(3, 2000, 2.0, 300_000));
        assertEquals(300_000, NotificationDispatchService.computeDelayMs(20, 2000, 2.0, 300_000));
    }

    @Test
    void jmsPriorityMapsBusinessPriority() {
        assertEquals(9, OutboxPublisher.toJmsPriority(Priority.HIGH));
        assertEquals(4, OutboxPublisher.toJmsPriority(Priority.NORMAL));
        assertEquals(1, OutboxPublisher.toJmsPriority(Priority.LOW));
    }
}
