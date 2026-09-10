package com.optimizesolux.notificationhub.infrastructure.provider;

import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Delivers a would-be SMS over the hub SMTP (Mailpit locally, Resend in prod) so test traffic
 * does not consume SMS credits. Destination defaults to {@code sms@optimizesolux.com}.
 */
@Component
public class MailpitSmsBridge {

    private static final Logger log = LoggerFactory.getLogger(MailpitSmsBridge.class);
    static final String DEFAULT_MAIL_FROM = "noreply@optimizesolux.com";
    static final String DEFAULT_MAIL_TO = "sms@optimizesolux.com";

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
        String mailTo = resolveMailTo(cfg != null ? cfg.mailTo() : null);
        String phones = to == null ? "" : String.join(", ", to);
        String subject = "[SMS TEST] " + phones;
        String html = renderHtml(smsFrom, to, body);
        String providerId =
                emailProviderRegistry.active().send(mailFrom, List.of(mailTo), subject, html);
        log.info(
                "SMS intercepted to email smsFrom={} to={} mailTo={} providerId={}",
                smsFrom,
                to,
                mailTo,
                providerId);
        return providerId;
    }

    static String resolveMailTo(String configured) {
        if (StringUtils.hasText(configured)) {
            return configured.trim();
        }
        return DEFAULT_MAIL_TO;
    }

    static String renderHtml(String smsFrom, List<String> to, String body) {
        String phones =
                to == null
                        ? ""
                        : to.stream().map(MailpitSmsBridge::escape).collect(Collectors.joining(", "));
        return """
                <p><strong>SMS intercepté (environnement ≠ prod)</strong></p>
                <p>Aucun SMS réel n'a été envoyé — crédit SMS économisé. Copie envoyée à
                <code>sms@optimizesolux.com</code> via le SMTP du hub (Mailpit en local, Resend en prod).</p>
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
