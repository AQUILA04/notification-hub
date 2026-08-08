package com.optimize.notification.hub.client;

import com.optimize.notification.hub.model.Channel;
import com.optimize.notification.hub.model.CreateNotificationRequest;
import com.optimize.notification.hub.model.NotificationEventResponse;
import com.optimize.notification.hub.model.NotificationResponse;
import com.optimize.notification.hub.model.NotificationStatus;
import com.optimize.notification.hub.model.PageResponse;

import java.util.List;
import java.util.UUID;

/**
 * Client API to interact with Notification Hub.
 * Auto-configured when {@code optimize.notification.hub.base-url} is set.
 */
public interface NotificationHubClient {

    /**
     * Creates a notification (async accept — Hub returns {@code 202 ACCEPTED}).
     */
    NotificationResponse send(CreateNotificationRequest request);

    /**
     * Creates a notification with an idempotency key ({@code Idempotency-Key} header).
     */
    NotificationResponse send(CreateNotificationRequest request, String idempotencyKey);

    NotificationResponse get(UUID id);

    PageResponse<NotificationResponse> list(NotificationStatus status, Channel channel, int page, int size);

    default PageResponse<NotificationResponse> list(int page, int size) {
        return list(null, null, page, size);
    }

    List<NotificationEventResponse> events(UUID id);
}
