package com.optimize.notification.hub.client;

import com.optimize.notification.hub.model.Channel;
import com.optimize.notification.hub.model.CreateNotificationRequest;
import com.optimize.notification.hub.model.NotificationEventResponse;
import com.optimize.notification.hub.model.NotificationResponse;
import com.optimize.notification.hub.model.NotificationStatus;
import com.optimize.notification.hub.model.OtpSendRequest;
import com.optimize.notification.hub.model.OtpSendResponse;
import com.optimize.notification.hub.model.OtpVerifyRequest;
import com.optimize.notification.hub.model.OtpVerifyResponse;
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
     * For {@code SMS}, omit {@code environment} to let the starter inject {@code test} or {@code prod}
     * from the active Spring profile.
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

    /**
     * Generates an OTP, stores it server-side, and dispatches it (WhatsApp or SMS).
     * SMS uses the client environment ({@code test} by default): non-prod traffic is intercepted
     * to Mailpit instead of a paid SMS provider.
     */
    OtpSendResponse sendOtp(OtpSendRequest request);

    OtpSendResponse sendOtp(OtpSendRequest request, String idempotencyKey);

    /** Verifies a code against the active OTP session for the destination. */
    OtpVerifyResponse verifyOtp(OtpVerifyRequest request);
}
