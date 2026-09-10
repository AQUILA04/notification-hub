package com.optimizesolux.notificationhub.infrastructure.provider;

import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Delivers a would-be SMS to Mailpit (configured SMTP) so test traffic does not consume SMS
 * credits.
 */
@Component
public class MailpitSmsBridge {

    private static final Logger log = LoggerFactory.getLogger(MailpitSmsBridge.class);
    static final String DEFAULT_MAIL_FROM = "sms-test@notification-hub.local";

    private final EmailProviderRegistry emailProviderRegistry;
    private final NotificationHubProperties properties;

    public MailpitSmsBridge(
            EmailProviderRegistry emailProviderRegistry, NotificationHubProperties properties) {
        this.emailProviderRegistry = emailProviderRegistry;
        this.properties = properties;
    }

    public String send(String smsFrom, List<String> to, String body) throws Exception {
        NotificationHubProperties.SmsTest cfg =
                properties.sms() != null ? properties.sms().test() : null;
        String mailFrom =
                cfg != null && StringUtils.hasText(cfg.mailFrom())
                        ? cfg.mailFrom().trim()
                        : DEFAULT_MAIL_FROM;
        List<String> mailTo = resolveMailTo(to, cfg != null ? cfg.mailTo() : null);
        String phones = to == null ? "" : String.join(", ", to);
        String subject = "[SMS TEST] " + phones;
        String html = renderHtml(smsFrom, to, body);
        String providerId = emailProviderRegistry.active().send(mailFrom, mailTo, subject, html);
        log.info(
                "SMS intercepted to Mailpit smsFrom={} to={} mailTo={} providerId={}",
                smsFrom,
                to,
                mailTo,
                providerId);
        return providerId;
    }

    static List<String> resolveMailTo(List<String> phones, String configured) {
        if (StringUtils.hasText(configured)) {
            return List.of(configured.trim());
        }
        if (phones == null || phones.isEmpty()) {
            return List.of("sms-test@notification-hub.local");
        }
        List<String> derived = new ArrayList<>();
        for (String phone : phones) {
            derived.add(phoneToLocalPart(phone) + "@sms.test.notification-hub.local");
        }
        return derived;
    }

    static String phoneToLocalPart(String phone) {
        if (phone == null || phone.isBlank()) {
            return "unknown";
        }
        String digits = phone.replaceAll("[^0-9A-Za-z]", "");
        return digits.isEmpty() ? "unknown" : digits;
    }

    static String renderHtml(String smsFrom, List<String> to, String body) {
        String phones =
                to == null
                        ? ""
                        : to.stream().map(MailpitSmsBridge::escape).collect(Collectors.joining(", "));
        return """
                <p><strong>SMS intercepté (environnement ≠ prod)</strong></p>
                <p>Aucun SMS réel n'a été envoyé — crédit SMS économisé. Message visible dans Mailpit.</p>
                <ul>
                  <li>From (sender ID) : %s</li>
                  <li>To : %s</li>
                </ul>
                <pre>%s</pre>
                """
                .formatted(
                        escape(smsFrom),
                        phones,
                        escape(body));
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
