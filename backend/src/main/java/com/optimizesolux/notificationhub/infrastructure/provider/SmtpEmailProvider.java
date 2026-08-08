package com.optimizesolux.notificationhub.infrastructure.provider;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        prefix = "notification-hub.email",
        name = "provider",
        havingValue = "smtp",
        matchIfMissing = true)
public class SmtpEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailProvider.class);

    private final JavaMailSender mailSender;

    public SmtpEmailProvider(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String id() {
        return "smtp";
    }

    @Override
    public String send(String from, List<String> to, String subject, String body) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to.toArray(String[]::new));
        helper.setSubject(subject != null ? subject : "(no subject)");
        helper.setText(body != null ? body : "", true);
        mailSender.send(message);
        String providerId = "smtp-" + UUID.randomUUID();
        log.info("Email SMTP sent from={} to={} providerId={}", from, to, providerId);
        return providerId;
    }
}
