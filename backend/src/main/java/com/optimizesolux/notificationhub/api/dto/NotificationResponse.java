package com.optimizesolux.notificationhub.api.dto;

import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.NotificationStatus;
import com.optimizesolux.notificationhub.domain.Priority;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String tenantId,
        Channel channel,
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
