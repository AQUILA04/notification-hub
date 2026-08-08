package com.optimizesolux.notificationhub.infrastructure.provider;

import com.optimizesolux.notificationhub.domain.Channel;

import java.util.List;

/**
 * SPI d'envoi pour un canal (EMAIL, SMS, WHATSAPP…).
 * Le dispatch ne dépend que de cette interface + {@link ChannelSenderRegistry}.
 */
public interface ChannelSender {

    Channel channel();

    /** Identifiant technique du provider actif (ex. twilio, afriksms, smtp). */
    String providerId();

    String send(String from, List<String> to, String subject, String body) throws Exception;
}
