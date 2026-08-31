package com.optimizesolux.notificationhub.otp.application;

import com.optimizesolux.notificationhub.api.dto.CreateNotificationRequest;
import com.optimizesolux.notificationhub.api.dto.NotificationResponse;
import com.optimizesolux.notificationhub.application.AuditService;
import com.optimizesolux.notificationhub.application.NotificationService;
import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import com.optimizesolux.notificationhub.config.TenantContext;
import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.MessageType;
import com.optimizesolux.notificationhub.otp.api.dto.OtpSendRequest;
import com.optimizesolux.notificationhub.otp.api.dto.OtpSendResponse;
import com.optimizesolux.notificationhub.otp.api.dto.OtpVerifyRequest;
import com.optimizesolux.notificationhub.otp.api.dto.OtpVerifyResponse;
import com.optimizesolux.notificationhub.otp.domain.OtpSession;
import com.optimizesolux.notificationhub.otp.domain.OtpVerifyReason;
import com.optimizesolux.notificationhub.otp.infrastructure.OtpStore;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class OtpService {

    private final OtpStore otpStore;
    private final NotificationService notificationService;
    private final NotificationHubProperties properties;
    private final AuditService auditService;

    public OtpService(
            OtpStore otpStore,
            NotificationService notificationService,
            NotificationHubProperties properties,
            AuditService auditService) {
        this.otpStore = otpStore;
        this.notificationService = notificationService;
        this.properties = properties;
        this.auditService = auditService;
    }

    public OtpSendResponse send(OtpSendRequest request, String idempotencyKey, String appIdHeader) {
        NotificationHubProperties.Otp otpConfig = requireOtpEnabled();
        String tenantId = TenantContext.require();
        Channel channel = resolveChannel(request.channel(), otpConfig);
        String destination = PhoneNormalizer.normalize(request.to());
        Duration ttl = Duration.ofSeconds(otpConfig.ttlSeconds());
        Duration cooldown = Duration.ofSeconds(otpConfig.resendCooldownSeconds());

        otpStore.enforceResendCooldown(tenantId, channel, destination, cooldown);

        if (channel == Channel.SMS) {
            String smsFrom = properties.sms().defaultFrom();
            if (smsFrom == null || smsFrom.isBlank()) {
                throw new IllegalStateException(
                        "SMS OTP requires SMS_DEFAULT_FROM configuration");
            }
        }

        String code = OtpCodeGenerator.generate(otpConfig.length());
        UUID sessionId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(ttl);
        String codeHash = OtpHasher.hash(tenantId, destination, code);

        OtpSession session =
                new OtpSession(tenantId, sessionId, channel, destination, codeHash, expiresAt);
        otpStore.save(session, ttl);
        otpStore.markResendCooldown(tenantId, channel, destination, cooldown);
        otpStore.clearFailedAttempts(tenantId, channel, destination);

        Map<String, Object> metadata = new HashMap<>();
        if (request.metadata() != null) {
            metadata.putAll(request.metadata());
        }
        metadata.put("otpSessionId", sessionId.toString());
        metadata.put("messageType", MessageType.OTP.name());

        CreateNotificationRequest notification =
                buildNotificationRequest(channel, destination, code, metadata, otpConfig);
        NotificationResponse sent =
                notificationService.create(notification, idempotencyKey, appIdHeader);

        auditService.record(
                tenantId,
                "OTP_SEND",
                sessionId.toString(),
                Map.of("channel", channel.name(), "destination", mask(destination)));

        return new OtpSendResponse(sessionId, expiresAt, sent.id(), channel);
    }

    public OtpVerifyResponse verify(OtpVerifyRequest request) {
        NotificationHubProperties.Otp otpConfig = requireOtpEnabled();
        String tenantId = TenantContext.require();
        Channel channel = resolveChannel(request.channel(), otpConfig);
        String destination = PhoneNormalizer.normalize(request.to());
        Duration ttl = Duration.ofSeconds(otpConfig.ttlSeconds());

        Optional<OtpSession> session =
                request.sessionId() != null
                        ? otpStore.findBySessionId(tenantId, request.sessionId())
                        : otpStore.findByDestination(tenantId, channel, destination);

        if (session.isEmpty()) {
            auditService.record(
                    tenantId,
                    "OTP_VERIFY",
                    "none",
                    Map.of("valid", false, "reason", OtpVerifyReason.EXPIRED.name()));
            return OtpVerifyResponse.failure(OtpVerifyReason.EXPIRED);
        }

        OtpSession active = session.get();
        if (!active.destination().equals(destination)) {
            return OtpVerifyResponse.failure(OtpVerifyReason.INVALID);
        }
        if (request.sessionId() != null && !active.sessionId().equals(request.sessionId())) {
            return OtpVerifyResponse.failure(OtpVerifyReason.INVALID);
        }

        if (OtpHasher.matches(tenantId, destination, request.code(), active.codeHash())) {
            otpStore.delete(tenantId, active.channel(), destination, active.sessionId());
            otpStore.clearFailedAttempts(tenantId, channel, destination);
            auditService.record(
                    tenantId,
                    "OTP_VERIFY",
                    active.sessionId().toString(),
                    Map.of("valid", true));
            return OtpVerifyResponse.success();
        }

        int attempts =
                otpStore.incrementFailedAttempts(tenantId, channel, destination, ttl);
        if (attempts >= otpConfig.maxVerifyAttempts()) {
            otpStore.delete(tenantId, active.channel(), destination, active.sessionId());
            otpStore.clearFailedAttempts(tenantId, channel, destination);
            auditService.record(
                    tenantId,
                    "OTP_VERIFY",
                    active.sessionId().toString(),
                    Map.of("valid", false, "reason", OtpVerifyReason.MAX_ATTEMPTS.name()));
            return OtpVerifyResponse.failure(OtpVerifyReason.MAX_ATTEMPTS);
        }

        auditService.record(
                tenantId,
                "OTP_VERIFY",
                active.sessionId().toString(),
                Map.of("valid", false, "reason", OtpVerifyReason.INVALID.name()));
        return OtpVerifyResponse.failure(OtpVerifyReason.INVALID);
    }

    private CreateNotificationRequest buildNotificationRequest(
            Channel channel,
            String destination,
            String code,
            Map<String, Object> metadata,
            NotificationHubProperties.Otp otpConfig) {
        if (channel == Channel.WHATSAPP) {
            return new CreateNotificationRequest(
                    Channel.WHATSAPP,
                    null,
                    List.of(destination),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    metadata,
                    MessageType.OTP,
                    code);
        }
        String body =
                otpConfig
                        .smsBodyTemplate()
                        .replace("{{code}}", code)
                        .replace("{{ttlMinutes}}", String.valueOf(otpConfig.ttlSeconds() / 60));
        String from = properties.sms().defaultFrom();
        return new CreateNotificationRequest(
                Channel.SMS,
                from,
                List.of(destination),
                null,
                body,
                null,
                null,
                null,
                null,
                metadata,
                MessageType.OTP,
                code);
    }

    private Channel resolveChannel(Channel requested, NotificationHubProperties.Otp otpConfig) {
        if (requested != null) {
            return requested;
        }
        return Channel.valueOf(otpConfig.defaultChannel().toUpperCase());
    }

    private NotificationHubProperties.Otp requireOtpEnabled() {
        NotificationHubProperties.Otp otp = properties.otp();
        if (otp == null || !otp.enabled()) {
            throw new IllegalStateException("OTP module is disabled (notification-hub.otp.enabled=false)");
        }
        return otp;
    }

    static String mask(String destination) {
        if (destination.length() <= 4) {
            return "****";
        }
        return "****" + destination.substring(destination.length() - 4);
    }
}
