package com.optimizesolux.notificationhub.api;

import com.optimizesolux.notificationhub.domain.Channel;

/**
 * Canal demandé par un client alors qu'il n'est pas activé sur le hub.
 */
public class ChannelNotEnabledException extends RuntimeException {

    private final Channel channel;

    public ChannelNotEnabledException(Channel channel) {
        super(messageFor(channel));
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }

    public static String messageFor(Channel channel) {
        return "Le canal "
                + channel.name()
                + " n'est pas activé sur le service de notification pour le moment. "
                + "Ce hub n'utilise pas encore ce canal ; utilisez un canal disponible "
                + "(ex. EMAIL, SMS) ou contactez l'administrateur pour l'activer.";
    }
}
