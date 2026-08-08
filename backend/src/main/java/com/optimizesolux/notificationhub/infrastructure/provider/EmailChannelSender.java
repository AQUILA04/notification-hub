package com.optimizesolux.notificationhub.infrastructure.provider;

import com.optimizesolux.notificationhub.domain.Channel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmailChannelSender implements ChannelSender {

    private final EmailProviderRegistry emailProviderRegistry;

    public EmailChannelSender(EmailProviderRegistry emailProviderRegistry) {
        this.emailProviderRegistry = emailProviderRegistry;
    }

    @Override
    public Channel channel() {
        return Channel.EMAIL;
    }

    @Override
    public String providerId() {
        return emailProviderRegistry.active().id();
    }

    @Override
    public String send(String from, List<String> to, String subject, String body) throws Exception {
        return emailProviderRegistry.active().send(from, to, subject, body);
    }
}
