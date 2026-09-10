package com.optimize.notification.hub.model;

import java.util.List;
import java.util.Map;

/**
 * Payload to create a notification on the Hub ({@code POST /v1/notifications}).
 */
public record CreateNotificationRequest(
        Channel channel,
        String from,
        List<String> to,
        String subject,
        String body,
        String templateName,
        Map<String, Object> templateData,
        Priority priority,
        RetryPolicy retryPolicy,
        Map<String, Object> metadata,
        MessageType messageType,
        String otpCode,
        NotificationEnvironment environment
) {
    public record RetryPolicy(Integer maxAttempts) {}

    public CreateNotificationRequest withEnvironment(NotificationEnvironment environment) {
        return new CreateNotificationRequest(
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
                environment);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Convenience builder for WhatsApp OTP (uses hub-configured ContentSid and default from). */
    public static CreateNotificationRequest whatsappOtp(String to, String otpCode) {
        return builder()
                .channel(Channel.WHATSAPP)
                .to(to)
                .messageType(MessageType.OTP)
                .otpCode(otpCode)
                .build();
    }

    public static final class Builder {
        private Channel channel;
        private String from;
        private List<String> to;
        private String subject;
        private String body;
        private String templateName;
        private Map<String, Object> templateData;
        private Priority priority;
        private RetryPolicy retryPolicy;
        private Map<String, Object> metadata;
        private MessageType messageType;
        private String otpCode;
        private NotificationEnvironment environment;

        public Builder channel(Channel channel) {
            this.channel = channel;
            return this;
        }

        public Builder from(String from) {
            this.from = from;
            return this;
        }

        public Builder to(List<String> to) {
            this.to = to;
            return this;
        }

        public Builder to(String... to) {
            this.to = List.of(to);
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder templateName(String templateName) {
            this.templateName = templateName;
            return this;
        }

        public Builder templateData(Map<String, Object> templateData) {
            this.templateData = templateData;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder messageType(MessageType messageType) {
            this.messageType = messageType;
            return this;
        }

        public Builder otpCode(String otpCode) {
            this.otpCode = otpCode;
            return this;
        }

        public Builder environment(NotificationEnvironment environment) {
            this.environment = environment;
            return this;
        }

        public CreateNotificationRequest build() {
            return new CreateNotificationRequest(
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
                    environment);
        }
    }
}
