package com.optimizesolux.notificationhub.otp.application;

import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.otp.api.dto.OtpSendRequest;
import com.optimizesolux.notificationhub.otp.api.dto.OtpSendResponse;
import com.optimizesolux.notificationhub.otp.infrastructure.InternalOtpProvider;
import com.optimizesolux.notificationhub.otp.infrastructure.OtpProvider;
import com.optimizesolux.notificationhub.otp.infrastructure.OtpProviderRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock private OtpProviderRegistry registry;
    @Mock private InternalOtpProvider internal;
    @Mock private OtpProvider twilioVerify;

    @Test
    void smsWithoutEnvUsesInternalToAvoidPaidSms() {
        OtpService service = new OtpService(registry, internal, properties("twilio-verify"));
        OtpSendRequest request = new OtpSendRequest("+22890909090", Channel.SMS, null);
        when(internal.send(any(), any(), any())).thenReturn(sampleResponse());

        service.send(request, "key", null);

        verify(internal).send(eq(request), eq("key"), eq(null));
        verify(registry, never()).require();
    }

    @Test
    void smsProdUsesConfiguredProvider() {
        OtpService service = new OtpService(registry, internal, properties("twilio-verify"));
        OtpSendRequest request =
                new OtpSendRequest(
                        "+22890909090",
                        Channel.SMS,
                        null,
                        com.optimizesolux.notificationhub.domain.NotificationEnvironment.PROD);
        when(registry.require()).thenReturn(twilioVerify);
        when(twilioVerify.send(any(), any(), any())).thenReturn(sampleResponse());

        service.send(request, null, null);

        verify(twilioVerify).send(eq(request), eq(null), eq(null));
        verify(internal, never()).send(any(), any(), any());
    }

    private static NotificationHubProperties properties(String otpProvider) {
        return new NotificationHubProperties(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new NotificationHubProperties.Otp(
                        true,
                        otpProvider,
                        "VAtest",
                        true,
                        null,
                        null,
                        6,
                        300,
                        5,
                        60,
                        "SMS",
                        4,
                        "Code {{code}} (ref. {{reference}})"));
    }

    private static OtpSendResponse sampleResponse() {
        return new OtpSendResponse(
                UUID.randomUUID(), Instant.now(), UUID.randomUUID(), Channel.SMS, "internal", "ref");
    }
}
