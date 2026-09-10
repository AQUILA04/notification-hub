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

        String templateName = request.templateName();
        if (templateName == null || templateName.isBlank()) {
            templateName = properties.whatsapp().otpContentSid();
        }
        if (templateName == null || templateName.isBlank()) {
            throw new OtpConfigurationException(
                    "WhatsApp OTP requires TWILIO_WHATSAPP_OTP_CONTENT_SID or templateName (HX…)");
        }
        if (!templateName.startsWith("HX")) {
            throw new IllegalArgumentException(
                    "WhatsApp OTP templateName must be a Twilio ContentSid (HX…)");
        }

        String from = request.from();
        if (from == null || from.isBlank()) {
            from = properties.whatsapp().defaultFrom();
        }
        if (from == null || from.isBlank()) {
            throw new OtpConfigurationException(
                    "WhatsApp OTP requires from or TWILIO_WHATSAPP_FROM");
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
}
