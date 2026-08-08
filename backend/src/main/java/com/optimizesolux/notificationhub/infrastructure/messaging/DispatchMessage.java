package com.optimizesolux.notificationhub.infrastructure.messaging;

public record DispatchMessage(String notificationId, String tenantId, String channel) {}
