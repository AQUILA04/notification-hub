package com.optimizesolux.notificationhub.infrastructure.provider;

import com.optimizesolux.notificationhub.domain.Channel;

import java.util.List;

/**
 * Contrat d'un provider SMS. Une nouvelle intégration (AfrikSMS, Twilio, …)
 * = une nouvelle classe {@code @Component} implémentant cette interface.
 * Aucune modification du dispatch ni des autres providers.
 */
public interface SmsProvider {

    /** Clé config : notification-hub.sms.provider */
    String id();

    String send(String from, List<String> to, String body) throws Exception;

    default boolean supports(Channel channel) {
        return channel == Channel.SMS;
    }
}
