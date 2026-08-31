package com.optimizesolux.notificationhub.otp.application;

import com.optimizesolux.notificationhub.api.dto.CreateNotificationRequest;
import com.optimizesolux.notificationhub.api.dto.NotificationResponse;
import com.optimizesolux.notificationhub.application.AuditService;
import com.optimizesolux.notificationhub.application.NotificationService;
import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import com.optimizesolux.notificationhub.config.TenantContext;
import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.MessageType;
import com.optimizesolux.notificationhub.domain.NotificationStatus;
import com.optimizesolux.notificationhub.otp.api.dto.OtpSendRequest;
import com.optimizesolux.notificationhub.otp.api.dto.OtpVerifyRequest;
import com.optimizesolux.notificationhub.otp.domain.OtpSession;
import com.optimizesolux.notificationhub.otp.domain.OtpVerifyReason;
import com.optimizesolux.notificationhub.otp.infrastructure.OtpStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final String TENANT = "demo-tenant";
    private static final String DEST = "+22890909090";

    @Mock private OtpStore otpStore;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        NotificationHubProperties properties = properties();
        otpService = new OtpService(otpStore, notificationService, properties, auditService);
        TenantContext.set(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void sendGeneratesCodeAndDispatchesWhatsAppOtp() {
        doNothing().when(otpStore).enforceResendCooldown(any(), any(), any(), any());
        doNothing().when(otpStore).save(any(), any());
        doNothing().when(otpStore).markResendCooldown(any(), any(), any(), any());
        doNothing().when(otpStore).clearFailedAttempts(any(), any(), any());

        UUID notificationId = UUID.randomUUID();
        when(notificationService.create(any(), any(), any()))
                .thenReturn(
                        new NotificationResponse(
                                notificationId,
                                TENANT,
                                Channel.WHATSAPP,
                                NotificationStatus.QUEUED,
                                "+14155238886",
                                java.util.List.of(DEST),
                                null,
                                "HXaaa",
                                null,
                                0,
                                5,
                                null,
                                Map.of(),
                                Instant.now(),
                                Instant.now(),
                                null));

        var response = otpService.send(new OtpSendRequest(DEST, Channel.WHATSAPP, null), null, null);

        assertEquals(Channel.WHATSAPP, response.channel());
        assertEquals(notificationId, response.notificationId());

        ArgumentCaptor<CreateNotificationRequest> captor =
                ArgumentCaptor.forClass(CreateNotificationRequest.class);
        verify(notificationService).create(captor.capture(), eq(null), eq(null));
        CreateNotificationRequest sent = captor.getValue();
        assertEquals(MessageType.OTP, sent.messageType());
        assertEquals(Channel.WHATSAPP, sent.channel());
        assertTrue(sent.otpCode().matches("\\d{6}"));
    }

    @Test
    void verifyReturnsValidOnMatchingCode() {
        String code = "424242";
        UUID sessionId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(300);
        String hash = OtpHasher.hash(TENANT, DEST, code);
        OtpSession session =
                new OtpSession(TENANT, sessionId, Channel.WHATSAPP, DEST, hash, expiresAt);

        when(otpStore.findByDestination(TENANT, Channel.WHATSAPP, DEST)).thenReturn(Optional.of(session));
        doNothing().when(otpStore).delete(TENANT, Channel.WHATSAPP, DEST, sessionId);
        doNothing().when(otpStore).clearFailedAttempts(TENANT, Channel.WHATSAPP, DEST);

        var result =
                otpService.verify(new OtpVerifyRequest(DEST, code, Channel.WHATSAPP, null));

        assertTrue(result.valid());
        assertEquals(OtpVerifyReason.VALID, result.reason());
    }

    @Test
    void verifyReturnsInvalidOnWrongCode() {
        UUID sessionId = UUID.randomUUID();
        OtpSession session =
                new OtpSession(
                        TENANT,
                        sessionId,
                        Channel.WHATSAPP,
                        DEST,
                        OtpHasher.hash(TENANT, DEST, "424242"),
                        Instant.now().plusSeconds(300));

        when(otpStore.findByDestination(TENANT, Channel.WHATSAPP, DEST)).thenReturn(Optional.of(session));
        when(otpStore.incrementFailedAttempts(TENANT, Channel.WHATSAPP, DEST, Duration.ofSeconds(300)))
                .thenReturn(1);

        var result =
                otpService.verify(new OtpVerifyRequest(DEST, "000000", Channel.WHATSAPP, null));

        assertFalse(result.valid());
        assertEquals(OtpVerifyReason.INVALID, result.reason());
    }

    @Test
    void verifyReturnsExpiredWhenNoSession() {
        when(otpStore.findByDestination(TENANT, Channel.WHATSAPP, DEST)).thenReturn(Optional.empty());

        var result =
                otpService.verify(new OtpVerifyRequest(DEST, "424242", Channel.WHATSAPP, null));

        assertFalse(result.valid());
        assertEquals(OtpVerifyReason.EXPIRED, result.reason());
    }

    private static NotificationHubProperties properties() {
        return new NotificationHubProperties(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new NotificationHubProperties.Whatsapp(
                        "twilio", null, null, null, "+14155238886", "HXaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
                null,
                null,
                null,
                null,
                new NotificationHubProperties.Otp(
                        true, 6, 300, 5, 60, "WHATSAPP", "Code {{code}}"));
    }
}
