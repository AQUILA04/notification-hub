package com.optimizesolux.notificationhub.api.dto;

import com.optimizesolux.notificationhub.domain.Channel;

import java.time.Instant;
import java.util.UUID;

public record TemplateResponse(
        UUID id,
        String name,
        int version,
        Channel channel,
        String engine,
        String subjectTemplate,
        String bodyTemplate,
        boolean active,
        Instant createdAt
) {}
