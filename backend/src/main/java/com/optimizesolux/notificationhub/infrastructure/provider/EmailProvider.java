package com.optimizesolux.notificationhub.infrastructure.provider;

import java.util.List;

/** SPI email — une classe par fournisseur (SMTP, Brevo, …). */
public interface EmailProvider {

    String id();

    String send(String from, List<String> to, String subject, String body) throws Exception;
}
