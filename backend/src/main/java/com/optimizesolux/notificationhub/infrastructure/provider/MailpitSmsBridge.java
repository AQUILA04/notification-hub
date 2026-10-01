package com.optimizesolux.notificationhub.infrastructure.provider;

import com.optimizesolux.notificationhub.application.SmsTestRecipientService;
import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Delivers a would-be SMS over the hub SMTP (Mailpit locally, Resend in prod) so test traffic
 * does not consume SMS credits. Destinations come from the global sms_test_recipients list,
 * with fallback to {@code SMS_TEST_MAIL_TO} / {@code sms@optimizesolux.com}.
 */
@Component
public class MailpitSmsBridge {

    private static final Logger log = LoggerFactory.getLogger(MailpitSmsBridge.class);
    static final String DEFAULT_MAIL_FROM = "noreply@optimizesolux.com";
    static final String DEFAULT_MAIL_TO = "sms@optimizesolux.com";

    private final EmailProviderRegistry emailProviderRegistry;
    private final NotificationHubProperties properties;
    private final SmsTestRecipientService smsTestRecipientService;

    public MailpitSmsBridge(
            EmailProviderRegistry emailProviderRegistry,
            NotificationHubProperties properties,
            SmsTestRecipientService smsTestRecipientService) {
        this.emailProviderRegistry = emailProviderRegistry;
        this.properties = properties;
        this.smsTestRecipientService = smsTestRecipientService;
    }

    public String send(String smsFrom, List<String> to, String body) throws Exception {
        NotificationHubProperties.SmsTest cfg =
                properties.sms() != null ? properties.sms().test() : null;
        String mailFrom =
                cfg != null && StringUtils.hasText(cfg.mailFrom())
                        ? cfg.mailFrom().trim()
                        : DEFAULT_MAIL_FROM;
        List<String> recipients =
                resolveRecipients(
                        smsTestRecipientService.activeEmails(),
                        cfg != null ? cfg.mailTo() : null);
        String phones = to == null ? "" : String.join(", ", to);
        String subject = "[SMS TEST] " + phones;
        String html = renderHtml(smsFrom, to, body, recipients);
        String providerId =
                emailProviderRegistry.active().send(mailFrom, recipients, subject, html);
        log.info(
                "SMS intercepted to email smsFrom={} to={} mailTo={} providerId={}",
                smsFrom,
                to,
                recipients,
                providerId);
        return providerId;
    }

    /**
     * Prefer the DB distribution list; if empty, fall back to configured {@code mail-to}, then
     * the hard-coded default inbox.
     */
    static List<String> resolveRecipients(List<String> fromDb, String configured) {
        if (fromDb != null && !fromDb.isEmpty()) {
            return List.copyOf(fromDb);
        }
        if (StringUtils.hasText(configured)) {
            return List.of(configured.trim());
        }
        return List.of(DEFAULT_MAIL_TO);
    }

    static String renderHtml(
            String smsFrom, List<String> to, String body, List<String> recipients) {
        String phones =
                to == null
                        ? ""
                        : to.stream().map(MailpitSmsBridge::escape).collect(Collectors.joining(", "));
        String mailTo =
                recipients == null
                        ? ""
                        : recipients.stream()
                                .map(MailpitSmsBridge::escape)
                                .collect(Collectors.joining(", "));
        return """
                <p><strong>SMS intercepté (environnement ≠ prod)</strong></p>
                <p>Aucun SMS réel n'a été envoyé — crédit SMS économisé. Copie envoyée à la liste
                de diffusion SMS test (<code>%s</code>) via le SMTP du hub (Mailpit en local,
                Resend en prod).</p>
                <ul>
                  <li>From (sender ID) : %s</li>
                  <li>To : %s</li>
                </ul>
                <pre>%s</pre>
                """
                .formatted(mailTo, escape(smsFrom), phones, escape(body));
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
