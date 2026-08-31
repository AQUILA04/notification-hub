package com.optimizesolux.notificationhub.otp.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.otp.api.dto.OtpSendRequest;
import com.optimizesolux.notificationhub.otp.api.dto.OtpSendResponse;
import com.optimizesolux.notificationhub.otp.api.dto.OtpVerifyRequest;
import com.optimizesolux.notificationhub.otp.api.dto.OtpVerifyResponse;
import com.optimizesolux.notificationhub.otp.application.PhoneNormalizer;
import com.optimizesolux.notificationhub.otp.domain.OtpVerifyReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * OTP via Twilio Verify API — generation, delivery, expiry and checks delegated to Twilio.
 * No Content template or Redis storage required.
 */
@Component
public class TwilioVerifyOtpProvider implements OtpProvider {

    private static final Logger log = LoggerFactory.getLogger(TwilioVerifyOtpProvider.class);
    private static final int DEFAULT_TTL_SECONDS = 600;

    private final NotificationHubProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public TwilioVerifyOtpProvider(
            NotificationHubProperties properties,
            RestTemplateBuilder builder,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate =
                builder
                        .setConnectTimeout(Duration.ofSeconds(5))
                        .setReadTimeout(Duration.ofSeconds(20))
                        .build();
    }

    @Override
    public String id() {
        return "twilio-verify";
    }

    @Override
    public OtpSendResponse send(OtpSendRequest request, String idempotencyKey, String appIdHeader) {
        NotificationHubProperties.Otp otp = properties.otp();
        String serviceSid = requireServiceSid(otp);
        String destination = PhoneNormalizer.normalize(request.to());
        Channel channel = resolveChannel(request.channel(), otp);
        String verifyChannel = toVerifyChannel(channel, otp);

        String url =
                "https://verify.twilio.com/v2/Services/" + serviceSid + "/Verifications";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", destination);
        form.add("Channel", verifyChannel);

        if (channel == Channel.WHATSAPP && otp.twilioVerifyWhatsappSmsFallback()) {
            try {
                String config =
                        objectMapper.writeValueAsString(
                                Map.of(
                                        "whatsapp", Map.of("enabled", true),
                                        "sms", Map.of("enabled", true)));
                form.add("ChannelConfiguration", config);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to encode ChannelConfiguration", e);
            }
        }

        Map<?, ?> body = postForm(url, form);
        String verificationSid = stringField(body, "sid");
        Instant expiresAt = Instant.now().plusSeconds(DEFAULT_TTL_SECONDS);

        log.info(
                "Twilio Verify sent to={} channel={} sid={}",
                mask(destination),
                verifyChannel,
                verificationSid);

        return new OtpSendResponse(
                UUID.randomUUID(),
                expiresAt,
                null,
                channel,
                id(),
                verificationSid);
    }

    @Override
    public OtpVerifyResponse verify(OtpVerifyRequest request) {
        NotificationHubProperties.Otp otp = properties.otp();
        String serviceSid = requireServiceSid(otp);
        String destination = PhoneNormalizer.normalize(request.to());

        String url =
                "https://verify.twilio.com/v2/Services/" + serviceSid + "/VerificationCheck";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", destination);
        form.add("Code", request.code().trim());

        try {
            Map<?, ?> body = postForm(url, form);
            String status = stringField(body, "status");
            if ("approved".equalsIgnoreCase(status)) {
                return OtpVerifyResponse.success();
            }
            if ("canceled".equalsIgnoreCase(status)) {
                return OtpVerifyResponse.failure(OtpVerifyReason.EXPIRED);
            }
            return OtpVerifyResponse.failure(OtpVerifyReason.INVALID);
        } catch (HttpStatusCodeException ex) {
            int code = ex.getStatusCode().value();
            String responseBody = ex.getResponseBodyAsString();
            if (code == 404) {
                return OtpVerifyResponse.failure(OtpVerifyReason.EXPIRED);
            }
            if (responseBody.contains("60202")) {
                return OtpVerifyResponse.failure(OtpVerifyReason.MAX_ATTEMPTS);
            }
            if (responseBody.contains("60203")) {
                throw new IllegalStateException(
                        "Twilio Verify rate limit reached for this number — retry later");
            }
            throw new IllegalStateException(
                    "Twilio Verify check failed: HTTP " + code + " " + responseBody);
        }
    }

    private Map<?, ?> postForm(String url, MultiValueMap<String, String> form) {
        String sid = requireAccountSid();
        String token = requireAuthToken();
        String auth =
                Base64.getEncoder()
                        .encodeToString((sid + ":" + token).getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + auth);

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response =
                restTemplate.postForEntity(url, new HttpEntity<>(form, headers), Map.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Twilio Verify returned " + response.getStatusCode());
        }
        return response.getBody();
    }

    private String toVerifyChannel(Channel channel, NotificationHubProperties.Otp otp) {
        return switch (channel) {
            case SMS -> "sms";
            case WHATSAPP -> "whatsapp";
            case EMAIL -> throw new IllegalArgumentException(
                    "Twilio Verify email channel is not mapped in notification-hub OTP API");
        };
    }

    private Channel resolveChannel(Channel requested, NotificationHubProperties.Otp otp) {
        if (requested != null) {
            return requested;
        }
        return Channel.valueOf(otp.defaultChannel().toUpperCase());
    }

    private String requireServiceSid(NotificationHubProperties.Otp otp) {
        String sid = otp.twilioVerifyServiceSid();
        if (sid == null || sid.isBlank()) {
            throw new IllegalStateException(
                    "Twilio Verify requires TWILIO_VERIFY_SERVICE_SID (VA…)");
        }
        return sid.trim();
    }

    private String requireAccountSid() {
        String sid =
                firstNonBlank(
                        properties.otp() != null ? properties.otp().twilioAccountSid() : null,
                        properties.whatsapp().accountSid(),
                        properties.sms().accountSid());
        if (sid == null || sid.isBlank()) {
            throw new IllegalStateException("Twilio Verify requires TWILIO_ACCOUNT_SID");
        }
        return sid;
    }

    private String requireAuthToken() {
        String token =
                firstNonBlank(
                        properties.otp() != null ? properties.otp().twilioAuthToken() : null,
                        properties.whatsapp().authToken(),
                        properties.sms().authToken());
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Twilio Verify requires TWILIO_AUTH_TOKEN");
        }
        return token;
    }

    private static String stringField(Map<?, ?> body, String key) {
        Object value = body.get(key);
        return value != null ? value.toString() : null;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static String mask(String destination) {
        if (destination.length() <= 4) {
            return "****";
        }
        return "****" + destination.substring(destination.length() - 4);
    }
}
