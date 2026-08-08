package com.optimizesolux.notificationhub.application;

import com.optimizesolux.notificationhub.api.ChannelNotEnabledException;
import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import com.optimizesolux.notificationhub.domain.Channel;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ChannelAvailabilityService {

    private final NotificationHubProperties properties;

    public ChannelAvailabilityService(NotificationHubProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled(Channel channel) {
        NotificationHubProperties.Channels channels = properties.channels();
        return switch (channel) {
            case EMAIL -> channels.email();
            case SMS -> channels.sms();
            case WHATSAPP -> channels.whatsapp();
        };
    }

    public void requireEnabled(Channel channel) {
        if (!isEnabled(channel)) {
            throw new ChannelNotEnabledException(channel);
        }
    }

    public Map<String, Boolean> snapshot() {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (Channel channel : Channel.values()) {
            map.put(channel.name(), isEnabled(channel));
        }
        return map;
    }
}
