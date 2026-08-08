package com.optimizesolux.notificationhub.infrastructure.provider;

import com.optimizesolux.notificationhub.domain.Channel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Façade canal SMS : délègue au {@link SmsProvider} sélectionné par config.
 * Remplacer Twilio par AfrikSMS = nouveau {@link SmsProvider}, pas de changement ici.
 */
@Component
public class SmsChannelSender implements ChannelSender {

    private final SmsProviderRegistry smsProviderRegistry;

    public SmsChannelSender(SmsProviderRegistry smsProviderRegistry) {
        this.smsProviderRegistry = smsProviderRegistry;
    }

    @Override
    public Channel channel() {
        return Channel.SMS;
    }

    @Override
    public String providerId() {
        return smsProviderRegistry.active().id();
    }

    @Override
    public String send(String from, List<String> to, String subject, String body) throws Exception {
        String text = body != null ? body : "";
        return smsProviderRegistry.active().send(from, to, text);
    }
}
