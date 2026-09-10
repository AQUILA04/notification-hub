package com.optimize.notification.hub.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String tenantId,
        Channel channel,
        NotificationEnvironment environment,
        NotificationStatus status,
        String from,
        List<String> to,
        String subject,
        String templateName,
        Priority priority,
        int attemptCount,
        int maxAttempts,
        String lastError,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt,
        Instant sentAt
) {}
