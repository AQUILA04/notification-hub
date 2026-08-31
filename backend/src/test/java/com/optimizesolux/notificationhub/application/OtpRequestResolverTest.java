package com.optimizesolux.notificationhub.application;

import com.optimizesolux.notificationhub.api.OtpConfigurationException;
import com.optimizesolux.notificationhub.api.dto.CreateNotificationRequest;
import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.MessageType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OtpRequestResolverTest {

    private static final String CONTENT_SID = "HXaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String DEFAULT_FROM = "+14155238886";

    private NotificationHubProperties properties() {
        return new NotificationHubProperties(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new NotificationHubProperties.Whatsapp(
                        "twilio", null, null, null, DEFAULT_FROM, CONTENT_SID),
                null,
                null,
                null,
                null);
    }

    @Test
    void resolvesWhatsAppOtpFromConfig() {
        CreateNotificationRequest input =
                new CreateNotificationRequest(
                        Channel.WHATSAPP,
                        null,
                        List.of("+22890909090"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        MessageType.OTP,
                        "424242");

        CreateNotificationRequest resolved = OtpRequestResolver.resolve(input, properties());

        assertEquals(DEFAULT_FROM, resolved.from());
        assertEquals(CONTENT_SID, resolved.templateName());
        assertEquals("424242", resolved.templateData().get("1"));
        assertEquals(MessageType.OTP.name(), resolved.metadata().get("messageType"));
    }

    @Test
    void allowsExplicitContentSidOverride() {
        String overrideSid = "HXbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
        CreateNotificationRequest input =
                new CreateNotificationRequest(
                        Channel.WHATSAPP,
                        DEFAULT_FROM,
                        List.of("+22890909090"),
                        null,
                        null,
                        overrideSid,
                        null,
                        null,
                        null,
                        null,
                        MessageType.OTP,
                        "9999");

        CreateNotificationRequest resolved = OtpRequestResolver.resolve(input, properties());

        assertEquals(overrideSid, resolved.templateName());
        assertEquals("9999", resolved.templateData().get("1"));
    }

    @Test
    void rejectsMissingOtpCode() {
        CreateNotificationRequest input =
                new CreateNotificationRequest(
                        Channel.WHATSAPP,
                        DEFAULT_FROM,
                        List.of("+22890909090"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        MessageType.OTP,
                        null);

        assertThrows(
                IllegalArgumentException.class, () -> OtpRequestResolver.resolve(input, properties()));
    }

    @Test
    void rejectsInvalidOtpCode() {
        CreateNotificationRequest input =
                new CreateNotificationRequest(
                        Channel.WHATSAPP,
                        DEFAULT_FROM,
                        List.of("+22890909090"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        MessageType.OTP,
                        "abc");

        assertThrows(
                IllegalArgumentException.class, () -> OtpRequestResolver.resolve(input, properties()));
    }

    @Test
    void rejectsBodyWithOtp() {
        CreateNotificationRequest input =
                new CreateNotificationRequest(
                        Channel.WHATSAPP,
                        DEFAULT_FROM,
                        List.of("+22890909090"),
                        null,
                        "OTP 4242",
                        null,
                        null,
                        null,
                        null,
                        null,
                        MessageType.OTP,
                        "424242");

        assertThrows(
                IllegalArgumentException.class, () -> OtpRequestResolver.resolve(input, properties()));
    }

    @Test
    void rejectsMissingContentSidConfig() {
        NotificationHubProperties noSid =
                new NotificationHubProperties(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new NotificationHubProperties.Whatsapp(
                                "twilio", null, null, null, DEFAULT_FROM, null),
                        null,
                        null,
                        null,
                        null);

        CreateNotificationRequest input =
                new CreateNotificationRequest(
                        Channel.WHATSAPP,
                        DEFAULT_FROM,
                        List.of("+22890909090"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        MessageType.OTP,
                        "424242");

        assertThrows(
                OtpConfigurationException.class, () -> OtpRequestResolver.resolve(input, noSid));
    }

    @Test
    void rejectsMissingFromWhenNotConfigured() {
        NotificationHubProperties noFrom =
                new NotificationHubProperties(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new NotificationHubProperties.Whatsapp(
                                "twilio", null, null, null, null, CONTENT_SID),
                        null,
                        null,
                        null,
                        null);

        CreateNotificationRequest input =
                new CreateNotificationRequest(
                        Channel.WHATSAPP,
                        null,
                        List.of("+22890909090"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        MessageType.OTP,
                        "424242");

        assertThrows(
                OtpConfigurationException.class, () -> OtpRequestResolver.resolve(input, noFrom));
    }

    @Test
    void passthroughNonOtpRequests() {
        CreateNotificationRequest input =
                new CreateNotificationRequest(
                        Channel.SMS,
                        "MyBrand",
                        List.of("+22890909090"),
                        null,
                        "OTP 4242",
                        null,
                        null,
                        null,
                        null,
                        Map.of("k", "v"));

        CreateNotificationRequest resolved = OtpRequestResolver.resolve(input, properties());

        assertEquals(input, resolved);
    }

    @Test
    void preservesExistingMetadata() {
        CreateNotificationRequest input =
                new CreateNotificationRequest(
                        Channel.WHATSAPP,
                        DEFAULT_FROM,
                        List.of("+22890909090"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Map.of("appId", "auth-service"),
                        MessageType.OTP,
                        "424242");

        CreateNotificationRequest resolved = OtpRequestResolver.resolve(input, properties());

        assertEquals("auth-service", resolved.metadata().get("appId"));
        assertEquals(MessageType.OTP.name(), resolved.metadata().get("messageType"));
    }
}
