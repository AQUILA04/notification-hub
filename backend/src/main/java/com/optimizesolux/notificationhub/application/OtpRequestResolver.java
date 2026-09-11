package com.optimizesolux.notificationhub.application;

import com.optimizesolux.notificationhub.api.OtpConfigurationException;
import com.optimizesolux.notificationhub.api.dto.CreateNotificationRequest;
import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.MessageType;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Normalises OTP requests before validation and persistence.
 */
public final class OtpRequestResolver {

    private static final Pattern OTP_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9]{4,15}$");

    private OtpRequestResolver() {}

    public static CreateNotificationRequest resolve(
            CreateNotificationRequest request, NotificationHubProperties properties) {
        if (request.messageType() != MessageType.OTP) {
            return request;
        }
        if (request.channel() == Channel.WHATSAPP) {
            return resolveWhatsAppOtp(request, properties);
        }
        return request;
    }

    private static CreateNotificationRequest resolveWhatsAppOtp(
            CreateNotificationRequest request, NotificationHubProperties properties) {
        String otpCode = request.otpCode();
        if (otpCode == null || otpCode.isBlank()) {
            throw new IllegalArgumentException("WhatsApp OTP requires otpCode");
        }
        if (!OTP_CODE_PATTERN.matcher(otpCode).matches()) {
            throw new IllegalArgumentException(
                    "otpCode must be 4–15 alphanumeric characters (Twilio/Meta limit)");
        }
        if (request.body() != null && !request.body().isBlank()) {
            throw new IllegalArgumentException("WhatsApp OTP must not include body; use otpCode");
        }

        boolean meta = isMetaProvider(properties);

        String templateName = request.templateName();
        if (templateName == null || templateName.isBlank()) {
            templateName = defaultOtpTemplate(properties, meta);
        }
        if (templateName == null || templateName.isBlank()) {
            throw new OtpConfigurationException(
                    meta
                            ? "WhatsApp OTP requires WHATSAPP_OTP_TEMPLATE_NAME or templateName"
                            : "WhatsApp OTP requires TWILIO_WHATSAPP_OTP_CONTENT_SID or templateName (HX…)");
        }
        if (!meta && !templateName.startsWith("HX")) {
            throw new IllegalArgumentException(
                    "WhatsApp OTP templateName must be a Twilio ContentSid (HX…)");
        }

        String from = request.from();
        if (from == null || from.isBlank()) {
            from = properties.whatsapp() != null ? properties.whatsapp().defaultFrom() : null;
        }
        if (!meta && (from == null || from.isBlank())) {
            throw new OtpConfigurationException(
                    "WhatsApp OTP requires from or TWILIO_WHATSAPP_FROM");
        }
        // Meta: from optional (Phone Number ID is in Graph URL); use phoneNumberId as placeholder
        if (meta && (from == null || from.isBlank())) {
            NotificationHubProperties.Meta m = properties.whatsapp().meta();
            from = m != null && m.phoneNumberId() != null ? m.phoneNumberId() : "meta";
        }

        Map<String, Object> metadata = new HashMap<>();
        if (request.metadata() != null) {
            metadata.putAll(request.metadata());
        }
        metadata.put("messageType", MessageType.OTP.name());

        return new CreateNotificationRequest(
                request.channel(),
                from,
                request.to(),
                request.subject(),
                null,
                templateName,
                Map.of("1", otpCode),
                request.priority(),
                request.retryPolicy(),
                metadata,
                request.messageType(),
                otpCode,
                request.environment());
    }

    static boolean isMetaProvider(NotificationHubProperties properties) {
        if (properties.whatsapp() == null || properties.whatsapp().provider() == null) {
            return false;
        }
        return "meta".equalsIgnoreCase(properties.whatsapp().provider().trim());
    }

    private static String defaultOtpTemplate(NotificationHubProperties properties, boolean meta) {
        if (properties.whatsapp() == null) {
            return null;
        }
        if (meta) {
            NotificationHubProperties.Meta m = properties.whatsapp().meta();
            return m != null ? m.otpTemplateName() : null;
        }
        return properties.whatsapp().otpContentSid();
    }
}
