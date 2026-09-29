package com.optimizesolux.notificationhub.otp.infrastructure;

import com.optimizesolux.notificationhub.api.dto.CreateNotificationRequest;
import com.optimizesolux.notificationhub.api.dto.NotificationResponse;
import com.optimizesolux.notificationhub.application.AuditService;
import com.optimizesolux.notificationhub.application.NotificationService;
import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import com.optimizesolux.notificationhub.config.TenantContext;
import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.MessageType;
import com.optimizesolux.notificationhub.domain.NotificationEnvironment;
import com.optimizesolux.notificationhub.otp.api.dto.OtpSendRequest;
import com.optimizesolux.notificationhub.otp.api.dto.OtpSendResponse;
import com.optimizesolux.notificationhub.otp.api.dto.OtpVerifyRequest;
import com.optimizesolux.notificationhub.otp.api.dto.OtpVerifyResponse;
import com.optimizesolux.notificationhub.otp.application.OtpCodeGenerator;
import com.optimizesolux.notificationhub.otp.application.OtpHasher;
import com.optimizesolux.notificationhub.otp.application.OtpReferenceGenerator;
import com.optimizesolux.notificationhub.otp.application.PhoneNormalizer;
import com.optimizesolux.notificationhub.otp.domain.OtpSession;
import com.optimizesolux.notificationhub.otp.domain.OtpVerifyReason;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Hub-native OTP: Redis storage + notification pipeline dispatch. */
@Component
public class InternalOtpProvider implements OtpProvider {

    private final OtpStore otpStore;
    private final NotificationService notificationService;
    private final NotificationHubProperties properties;
    private final AuditService auditService;

    public InternalOtpProvider(
            OtpStore otpStore,
            NotificationService notificationService,
            NotificationHubProperties properties,
            AuditService auditService) {
        this.otpStore = otpStore;
        this.notificationService = notificationService;
        this.properties = properties;
        this.auditService = auditService;
    }

    @Override
    public String id() {
        return "internal";
    }

    @Override
    public OtpSendResponse send(OtpSendRequest request, String idempotencyKey, String appIdHeader) {
        NotificationHubProperties.Otp otpConfig = properties.otp();
        String tenantId = TenantContext.require();
        Channel channel = resolveChannel(request.channel(), otpConfig);
        String destination = PhoneNormalizer.normalize(request.to());
        Duration ttl = Duration.ofSeconds(otpConfig.ttlSeconds());
        Duration cooldown = Duration.ofSeconds(otpConfig.resendCooldownSeconds());
        NotificationEnvironment environment = NotificationEnvironment.from(request.environment());

        otpStore.enforceResendCooldown(tenantId, channel, destination, cooldown);

        if (channel == Channel.SMS && environment.isProd()) {
            String smsFrom = properties.sms().defaultFrom();
            if (smsFrom == null || smsFrom.isBlank()) {
                throw new IllegalStateException(
                        "SMS OTP requires SMS_DEFAULT_FROM configuration");
            }
        }

        String code = OtpCodeGenerator.generate(otpConfig.length());
        String reference = OtpReferenceGenerator.generate(otpConfig.referenceLength());
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
        metadata.put("otpReference", reference);

        CreateNotificationRequest notification =
                buildNotificationRequest(
                        channel, destination, code, reference, metadata, otpConfig, environment);
        NotificationResponse sent =
                notificationService.create(notification, idempotencyKey, appIdHeader);

        auditService.record(
                tenantId,
                "OTP_SEND",
                sessionId.toString(),
                Map.of(
                        "channel",
                        channel.name(),
                        "destination",
                        mask(destination),
                        "provider",
                        id(),
                        "reference",
                        reference));

        return new OtpSendResponse(
                sessionId, expiresAt, sent.id(), channel, id(), sessionId.toString(), reference);
    }

    public boolean hasActiveSession(OtpVerifyRequest request) {
        return findSession(request).isPresent();
    }

    @Override
    public OtpVerifyResponse verify(OtpVerifyRequest request) {
        NotificationHubProperties.Otp otpConfig = properties.otp();
        String tenantId = TenantContext.require();
        String destination = PhoneNormalizer.normalize(request.to());
        Duration ttl = Duration.ofSeconds(otpConfig.ttlSeconds());

        Optional<OtpSession> session = findSession(request);

        if (session.isEmpty()) {
            auditService.record(
                    tenantId,
                    "OTP_VERIFY",
                    "none",
                    Map.of("valid", false, "reason", OtpVerifyReason.EXPIRED.name(), "provider", id()));
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
            otpStore.clearFailedAttempts(tenantId, active.channel(), destination);
            auditService.record(
                    tenantId,
                    "OTP_VERIFY",
                    active.sessionId().toString(),
                    Map.of("valid", true, "provider", id()));
            return OtpVerifyResponse.success();
        }

        int attempts = otpStore.incrementFailedAttempts(tenantId, active.channel(), destination, ttl);
        if (attempts >= otpConfig.maxVerifyAttempts()) {
            otpStore.delete(tenantId, active.channel(), destination, active.sessionId());
            otpStore.clearFailedAttempts(tenantId, active.channel(), destination);
            auditService.record(
                    tenantId,
                    "OTP_VERIFY",
                    active.sessionId().toString(),
                    Map.of("valid", false, "reason", OtpVerifyReason.MAX_ATTEMPTS.name(), "provider", id()));
            return OtpVerifyResponse.failure(OtpVerifyReason.MAX_ATTEMPTS);
        }

        auditService.record(
                tenantId,
                "OTP_VERIFY",
                active.sessionId().toString(),
                Map.of("valid", false, "reason", OtpVerifyReason.INVALID.name(), "provider", id()));
        return OtpVerifyResponse.failure(OtpVerifyReason.INVALID);
    }

    private CreateNotificationRequest buildNotificationRequest(
            Channel channel,
            String destination,
            String code,
            String reference,
            Map<String, Object> metadata,
            NotificationHubProperties.Otp otpConfig,
            NotificationEnvironment environment) {
        if (channel == Channel.WHATSAPP) {
            // Les templates WhatsApp n'incluent pas {{reference}} ; la référence est
            // quand même renvoyée dans OtpSendResponse pour l'affichage côté app.
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
                    code,
                    environment);
        }
        String body =
                otpConfig
                        .smsBodyTemplate()
                        .replace("{{code}}", code)
                        .replace("{{ttlMinutes}}", String.valueOf(otpConfig.ttlSeconds() / 60))
                        .replace("{{reference}}", reference);
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
                code,
                environment);
    }

    private Optional<OtpSession> findSession(OtpVerifyRequest request) {
        String tenantId = TenantContext.require();
        if (request.sessionId() != null) {
            return otpStore.findBySessionId(tenantId, request.sessionId());
        }
        String destination = PhoneNormalizer.normalize(request.to());
        Channel channel = resolveChannel(request.channel(), properties.otp());
        Optional<OtpSession> found = otpStore.findByDestination(tenantId, channel, destination);
        if (found.isPresent()) {
            return found;
        }
        if (channel != Channel.SMS) {
            return otpStore.findByDestination(tenantId, Channel.SMS, destination);
        }
        return found;
    }

    private Channel resolveChannel(Channel requested, NotificationHubProperties.Otp otpConfig) {
        if (requested != null) {
            return requested;
        }
        return Channel.valueOf(otpConfig.defaultChannel().toUpperCase());
    }

    static String mask(String destination) {
        if (destination.length() <= 4) {
            return "****";
        }
        return "****" + destination.substring(destination.length() - 4);
    }
}
