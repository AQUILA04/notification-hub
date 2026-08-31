package com.optimizesolux.notificationhub.otp.infrastructure;

import com.optimizesolux.notificationhub.application.AuditService;
import com.optimizesolux.notificationhub.application.NotificationService;
import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import com.optimizesolux.notificationhub.config.TenantContext;
import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.MessageType;
import com.optimizesolux.notificationhub.domain.NotificationStatus;
import com.optimizesolux.notificationhub.api.dto.CreateNotificationRequest;
import com.optimizesolux.notificationhub.api.dto.NotificationResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalOtpProviderTest {

    private static final String TENANT = "demo-tenant";
    private static final String DEST = "+22890909090";

    @Mock private OtpStore otpStore;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;

    private InternalOtpProvider provider;

    @BeforeEach
    void setUp() {
        provider =
                new InternalOtpProvider(
                        otpStore, notificationService, properties(), auditService);
        TenantContext.set(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void sendGeneratesCodeAndDispatchesSmsOtp() {
        doNothing().when(otpStore).enforceResendCooldown(any(), any(), any(), any());
        doNothing().when(otpStore).save(any(), any());
        doNothing().when(otpStore).markResendCooldown(any(), any(), any(), any());
        doNothing().when(otpStore).clearFailedAttempts(any(), any(), any());

        java.util.UUID notificationId = java.util.UUID.randomUUID();
        when(notificationService.create(any(), any(), any()))
                .thenReturn(
                        new NotificationResponse(
                                notificationId,
                                TENANT,
                                Channel.SMS,
                                NotificationStatus.QUEUED,
                                "MyBrand",
                                java.util.List.of(DEST),
                                null,
                                null,
                                null,
                                0,
                                5,
                                null,
                                Map.of(),
                                Instant.now(),
                                Instant.now(),
                                null));

        var response =
                provider.send(
                        new com.optimizesolux.notificationhub.otp.api.dto.OtpSendRequest(
                                DEST, Channel.SMS, null),
                        null,
                        null);

        assertEquals(Channel.SMS, response.channel());
        assertEquals("internal", response.provider());

        ArgumentCaptor<CreateNotificationRequest> captor =
                ArgumentCaptor.forClass(CreateNotificationRequest.class);
        verify(notificationService).create(captor.capture(), eq(null), eq(null));
        assertTrue(captor.getValue().body().contains(captor.getValue().otpCode()));
    }

    private static NotificationHubProperties properties() {
        return new NotificationHubProperties(
                null,
                null,
                null,
                null,
                null,
                new NotificationHubProperties.Sms(
                        "logging", null, null, null, null, "MyBrand", null),
                null,
                null,
                null,
                null,
                null,
                null,
                new NotificationHubProperties.Otp(
                        true,
                        "internal",
                        null,
                        false,
                        null,
                        null,
                        6,
                        300,
                        5,
                        60,
                        "SMS",
                        "Code {{code}}"));
    }
}
