package com.optimizesolux.notificationhub.infrastructure.provider;

import com.optimizesolux.notificationhub.domain.Channel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "notification-hub.channels", name = "whatsapp", havingValue = "true")
public class WhatsAppChannelSender implements ChannelSender {

    private final WhatsAppProviderRegistry registry;

    public WhatsAppChannelSender(WhatsAppProviderRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Channel channel() {
        return Channel.WHATSAPP;
    }

    @Override
    public String providerId() {
        return registry.active().id();
    }

    @Override
    public String send(String from, List<String> to, String subject, String body) throws Exception {
        // subject unused; body = text or ContentSid (HX…)
        return registry.active().send(from, to, body, Map.of());
    }

    public String sendWithTemplateData(
            String from, List<String> to, String contentSidOrBody, Map<String, Object> templateData)
            throws Exception {
        return registry.active().send(from, to, contentSidOrBody, templateData);
    }
}
