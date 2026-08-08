package com.optimizesolux.notificationhub.api.dto;

import com.optimizesolux.notificationhub.domain.NotificationEventType;

import java.time.Instant;
import java.util.Map;

public record NotificationEventResponse(
        long id,
        int sequenceNo,
        NotificationEventType eventType,
        Map<String, Object> payload,
        Instant occurredAt
) {}
