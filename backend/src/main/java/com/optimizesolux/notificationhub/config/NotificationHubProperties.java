package com.optimizesolux.notificationhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "notification-hub")
public record NotificationHubProperties(
        Queues queues,
        Outbox outbox,
        Retry retry,
        Security security,
        Channels channels,
        Sms sms,
        Email email,
        Whatsapp whatsapp,
        Cors cors,
        Quota quota,
        CircuitBreaker circuitBreaker,
        Cost cost,
        Otp otp) {
    public NotificationHubProperties {
        if (queues == null) {
            queues = new Queues("nhub.email", "nhub.sms", "nhub.whatsapp", "nhub.dlq");
        }
        if (outbox == null) {
            outbox = new Outbox(500, 50);
        }
        if (retry == null) {
            retry = new Retry(5, 2000, 2.0, 300_000);
        }
        if (security == null) {
            security = new Security(false);
        }
        if (channels == null) {
            channels = new Channels(true, true, false);
        }
        if (sms == null) {
            sms =
                    new Sms(
                            "logging",
                            null,
                            null,
                            null,
                            null,
                            null,
                            new AfrikSms(
                                    null,
                                    null,
                                    "https://api.afriksms.com/api/web/web_v1/outbounds"));
        }
        if (sms.afriksms() == null) {
            sms =
                    new Sms(
                            sms.provider(),
                            sms.accountSid(),
                            sms.authToken(),
                            sms.httpUrl(),
                            sms.httpAuthHeader(),
                            sms.defaultFrom(),
                            new AfrikSms(
                                    null,
                                    null,
                                    "https://api.afriksms.com/api/web/web_v1/outbounds"));
        }
        if (email == null) {
            email = new Email("smtp");
        }
        if (whatsapp == null) {
            whatsapp = new Whatsapp("twilio", null, null, null, null, null);
        }
        if (cors == null) {
            cors = new Cors(new String[] {"http://localhost:4200"});
        }
        if (quota == null) {
            quota = new Quota(true, 120, Map.of("email", 200, "sms", 60, "whatsapp", 30));
        }
        if (circuitBreaker == null) {
            circuitBreaker = new CircuitBreaker(true, 5, 60);
        }
        if (cost == null) {
            // EUR estimés par message (indicatif ops)
            cost = new Cost(0.0001, 0.04, 0.05);
        }
        if (otp == null) {
            otp =
                    new Otp(
                            true,
                            "twilio-verify",
                            null,
                            true,
                            null,
                            null,
                            6,
                            300,
                            5,
                            60,
                            "SMS",
                            "Votre code de verification est {{code}}. Valide {{ttlMinutes}} minutes.");
        }
    }

    public record Queues(String email, String sms, String whatsapp, String dlq) {}

    public record Outbox(long pollIntervalMs, int batchSize) {}

    public record Retry(int defaultMaxAttempts, long initialDelayMs, double multiplier, long maxDelayMs) {}

    public record Security(boolean permitLocal) {}

    /** Activation métier des canaux exposés aux apps clientes. */
    public record Channels(boolean email, boolean sms, boolean whatsapp) {}

    /**
     * @param provider logging (dev) | afriksms (prod SMS) | twilio (optionnel) | http
     */
    public record Sms(
            String provider,
            String accountSid,
            String authToken,
            String httpUrl,
            String httpAuthHeader,
            String defaultFrom,
            AfrikSms afriksms) {}

    public record AfrikSms(String clientId, String apiKey, String baseUrl) {}

    /** @param provider smtp | brevo (future EmailProvider.id) */
    public record Email(String provider) {}

    /**
     * WhatsApp — Twilio only for P2.
     *
     * @param provider twilio
     * @param accountSid Twilio SID (peut réutiliser TWILIO_ACCOUNT_SID)
     * @param authToken Twilio token
     * @param statusCallbackUrl URL publique webhook (optionnel)
     * @param defaultFrom numéro WhatsApp expéditeur par défaut (sandbox ou prod)
     * @param otpContentSid ContentSid Twilio (HX…) pour template whatsapp/authentication
     */
    public record Whatsapp(
            String provider,
            String accountSid,
            String authToken,
            String statusCallbackUrl,
            String defaultFrom,
            String otpContentSid) {}

    public record Cors(String[] allowedOrigins) {}

    /**
     * Rate-limit Redis par tenant + canal + app (fenêtre 1 minute).
     *
     * @param enabled actif
     * @param defaultPerMinute plafond par défaut
     * @param perChannelPerMinute plafonds optionnels (clés email|sms|whatsapp)
     */
    public record Quota(boolean enabled, int defaultPerMinute, Map<String, Integer> perChannelPerMinute) {}

    /**
     * Circuit breaker Redis par canal/provider.
     *
     * @param enabled actif
     * @param failureThreshold échecs consécutifs avant OPEN
     * @param openDurationSeconds durée OPEN avant HALF_OPEN
     */
    public record CircuitBreaker(boolean enabled, int failureThreshold, int openDurationSeconds) {}

    /** Coût estimé par message (devise indicative, pour KPI cockpit). */
    public record Cost(double email, double sms, double whatsapp) {}

    /**
     * OTP module — generate, store, verify, and dispatch.
     *
     * @param enabled module actif
     * @param provider internal (Redis + notification pipeline) | twilio-verify
     * @param twilioVerifyServiceSid Verify Service SID (VA…) when provider=twilio-verify
     * @param twilioVerifyWhatsappSmsFallback fallback SMS si WhatsApp Verify échoue
     * @param twilioAccountSid optional override (sinon TWILIO_ACCOUNT_SID global)
     * @param twilioAuthToken optional override (sinon TWILIO_AUTH_TOKEN global)
     * @param length longueur du code (provider internal uniquement)
     * @param ttlSeconds TTL Redis (provider internal uniquement)
     * @param maxVerifyAttempts tentatives verify (provider internal uniquement)
     * @param resendCooldownSeconds cooldown renvoi (provider internal uniquement)
     * @param defaultChannel WHATSAPP ou SMS
     * @param smsBodyTemplate corps SMS internal provider
     */
    public record Otp(
            boolean enabled,
            String provider,
            String twilioVerifyServiceSid,
            boolean twilioVerifyWhatsappSmsFallback,
            String twilioAccountSid,
            String twilioAuthToken,
            int length,
            int ttlSeconds,
            int maxVerifyAttempts,
            int resendCooldownSeconds,
            String defaultChannel,
            String smsBodyTemplate) {}
}
