package com.optimizesolux.notificationhub.otp.application;

import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import com.optimizesolux.notificationhub.otp.api.dto.OtpSendRequest;
import com.optimizesolux.notificationhub.otp.api.dto.OtpSendResponse;
import com.optimizesolux.notificationhub.otp.api.dto.OtpVerifyRequest;
import com.optimizesolux.notificationhub.otp.api.dto.OtpVerifyResponse;
import com.optimizesolux.notificationhub.otp.infrastructure.OtpProviderRegistry;
import org.springframework.stereotype.Service;

@Service
public class OtpService {

    private final OtpProviderRegistry providerRegistry;
    private final NotificationHubProperties properties;

    public OtpService(OtpProviderRegistry providerRegistry, NotificationHubProperties properties) {
        this.providerRegistry = providerRegistry;
        this.properties = properties;
    }

    public OtpSendResponse send(OtpSendRequest request, String idempotencyKey, String appIdHeader) {
        requireOtpEnabled();
        return providerRegistry.require().send(request, idempotencyKey, appIdHeader);
    }

    public OtpVerifyResponse verify(OtpVerifyRequest request) {
        requireOtpEnabled();
        return providerRegistry.require().verify(request);
    }

    private NotificationHubProperties.Otp requireOtpEnabled() {
        NotificationHubProperties.Otp otp = properties.otp();
        if (otp == null || !otp.enabled()) {
            throw new IllegalStateException("OTP module is disabled (notification-hub.otp.enabled=false)");
        }
        return otp;
    }
}
