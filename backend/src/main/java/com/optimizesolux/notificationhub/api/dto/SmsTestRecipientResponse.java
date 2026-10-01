package com.optimizesolux.notificationhub.api.dto;

import java.time.Instant;
import java.util.UUID;

public record SmsTestRecipientResponse(
        UUID id, String email, Instant createdAt, String createdBy) {}
