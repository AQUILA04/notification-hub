package com.optimizesolux.notificationhub.otp.application;

import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.NotificationEnvironment;
import com.optimizesolux.notificationhub.otp.api.dto.OtpSendRequest;
import com.optimizesolux.notificationhub.otp.api.dto.OtpSendResponse;
import com.optimizesolux.notificationhub.otp.api.dto.OtpVerifyRequest;
import com.optimizesolux.notificationhub.otp.api.dto.OtpVerifyResponse;
import com.optimizesolux.notificationhub.otp.infrastructure.InternalOtpProvider;
import com.optimizesolux.notificationhub.otp.infrastructure.OtpProvider;
import com.optimizesolux.notificationhub.otp.infrastructure.OtpProviderRegistry;
import org.springframework.stereotype.Service;

@Service
public class OtpService {

    private final OtpProviderRegistry providerRegistry;
    private final InternalOtpProvider internalOtpProvider;
    private final NotificationHubProperties properties;

    public OtpService(
            OtpProviderRegistry providerRegistry,
            InternalOtpProvider internalOtpProvider,
            NotificationHubProperties properties) {
        this.providerRegistry = providerRegistry;
        this.internalOtpProvider = internalOtpProvider;
        this.properties = properties;
    }

    public OtpSendResponse send(OtpSendRequest request, String idempotencyKey, String appIdHeader) {
        requireOtpEnabled();
        NotificationEnvironment environment = NotificationEnvironment.from(request.environment());
        Channel channel = resolveChannel(request.channel());
        if (channel == Channel.SMS && !environment.isProd()) {
            // Avoid Twilio/Brevo SMS spend: generate locally and dispatch via Mailpit.
            return internalOtpProvider.send(request, idempotencyKey, appIdHeader);
        }
        return providerRegistry.require().send(request, idempotencyKey, appIdHeader);
    }

    public OtpVerifyResponse verify(OtpVerifyRequest request) {
        requireOtpEnabled();
        OtpProvider configured = providerRegistry.require();
        if (internalOtpProvider.hasActiveSession(request)) {
            return internalOtpProvider.verify(request);
        }
        return configured.verify(request);
    }

    private Channel resolveChannel(Channel requested) {
        if (requested != null) {
            return requested;
        }
        String configured = properties.otp().defaultChannel();
        return Channel.valueOf((configured != null ? configured : "SMS").toUpperCase());
    }

    private NotificationHubProperties.Otp requireOtpEnabled() {
        NotificationHubProperties.Otp otp = properties.otp();
        if (otp == null || !otp.enabled()) {
            throw new IllegalStateException("OTP module is disabled (notification-hub.otp.enabled=false)");
        }
        return otp;
    }
}
