package com.optimize.notification.hub.model;

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
