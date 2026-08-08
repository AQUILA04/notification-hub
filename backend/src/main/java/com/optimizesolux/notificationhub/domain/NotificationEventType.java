package com.optimizesolux.notificationhub.domain;

public enum NotificationEventType {
    RECEIVED,
    QUEUED,
    RENDERED,
    ATTEMPT_STARTED,
    ATTEMPT_SUCCEEDED,
    ATTEMPT_FAILED,
    DEAD_LETTERED,
    CANCELLED,
    PROVIDER_ACK,
    REPLAY_REQUESTED
}
