package com.optimizesolux.notificationhub.api;

import com.optimizesolux.notificationhub.application.ChannelAvailabilityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Découverte publique des canaux disponibles (apps clientes).
 */
@RestController
@RequestMapping("/v1/channels")
public class ChannelsController {

    private final ChannelAvailabilityService channelAvailabilityService;

    public ChannelsController(ChannelAvailabilityService channelAvailabilityService) {
        this.channelAvailabilityService = channelAvailabilityService;
    }

    @GetMapping
    public Map<String, Boolean> list() {
        return channelAvailabilityService.snapshot();
    }
}
