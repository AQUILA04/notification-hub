package com.optimizesolux.notificationhub.api.dto;

import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.MessageType;
import com.optimizesolux.notificationhub.domain.NotificationEnvironment;
import com.optimizesolux.notificationhub.domain.Priority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record CreateNotificationRequest(
        @NotNull Channel channel,
        String from,
        @NotEmpty List<@NotBlank String> to,
        String subject,
        String body,
        String templateName,
        Map<String, Object> templateData,
        Priority priority,
        @Valid RetryPolicyRequest retryPolicy,
        Map<String, Object> metadata,
        MessageType messageType,
        String otpCode,
        NotificationEnvironment environment
) {
    public CreateNotificationRequest {
        environment = NotificationEnvironment.from(environment);
    }

    /** Backward-compatible constructor without OTP / environment fields. */
    public CreateNotificationRequest(
            Channel channel,
            String from,
            List<String> to,
            String subject,
            String body,
            String templateName,
            Map<String, Object> templateData,
            Priority priority,
            RetryPolicyRequest retryPolicy,
            Map<String, Object> metadata) {
        this(
                channel,
                from,
                to,
                subject,
                body,
                templateName,
                templateData,
                priority,
                retryPolicy,
                metadata,
                null,
                null,
                null);
    }

    /** Backward-compatible constructor without environment. */
    public CreateNotificationRequest(
            Channel channel,
            String from,
            List<String> to,
            String subject,
            String body,
            String templateName,
            Map<String, Object> templateData,
            Priority priority,
            RetryPolicyRequest retryPolicy,
            Map<String, Object> metadata,
            MessageType messageType,
            String otpCode) {
        this(
                channel,
                from,
                to,
                subject,
                body,
                templateName,
                templateData,
                priority,
                retryPolicy,
                metadata,
                messageType,
                otpCode,
                null);
    }

    public record RetryPolicyRequest(Integer maxAttempts) {}
}
