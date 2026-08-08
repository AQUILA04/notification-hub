package com.optimizesolux.notificationhub.infrastructure.provider;

import com.optimizesolux.notificationhub.domain.Channel;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Résout le {@link ChannelSender} actif pour un canal.
 * Un bean {@link ChannelSender} par canal (EMAIL, SMS, …).
 */
@Component
public class ChannelSenderRegistry {

    private final Map<Channel, ChannelSender> senders = new EnumMap<>(Channel.class);

    public ChannelSenderRegistry(List<ChannelSender> channelSenders) {
        for (ChannelSender sender : channelSenders) {
            Channel channel = sender.channel();
            ChannelSender previous = senders.put(channel, sender);
            if (previous != null) {
                throw new IllegalStateException(
                        "Multiple ChannelSender beans for "
                                + channel
                                + ": "
                                + previous.getClass().getSimpleName()
                                + " and "
                                + sender.getClass().getSimpleName());
            }
        }
    }

    public ChannelSender require(Channel channel) {
        ChannelSender sender = senders.get(channel);
        if (sender == null) {
            throw new UnsupportedOperationException(channel + " sender not registered");
        }
        return sender;
    }
}
