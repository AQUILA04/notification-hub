package com.optimizesolux.notificationhub.api;

import com.optimizesolux.notificationhub.api.dto.KpiResponse;
import com.optimizesolux.notificationhub.application.ChannelAvailabilityService;
import com.optimizesolux.notificationhub.application.CircuitBreakerService;
import com.optimizesolux.notificationhub.application.KpiService;
import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/admin")
public class AdminController {

    private final KpiService kpiService;
    private final ChannelAvailabilityService channelAvailabilityService;
    private final CircuitBreakerService circuitBreakerService;
    private final NotificationHubProperties properties;

    public AdminController(
            KpiService kpiService,
            ChannelAvailabilityService channelAvailabilityService,
            CircuitBreakerService circuitBreakerService,
            NotificationHubProperties properties) {
        this.kpiService = kpiService;
        this.channelAvailabilityService = channelAvailabilityService;
        this.circuitBreakerService = circuitBreakerService;
        this.properties = properties;
    }

    @GetMapping("/kpi")
    public KpiResponse kpi(@RequestParam(defaultValue = "24") int windowHours) {
        return kpiService.snapshot(windowHours);
    }

    /** Canaux activés / désactivés — pour les apps clientes et le cockpit. */
    @GetMapping("/channels")
    public Map<String, Boolean> channels() {
        return channelAvailabilityService.snapshot();
    }

    /** État des circuit breakers par canal:provider. */
    @GetMapping("/circuits")
    public Map<String, String> circuits() {
        return circuitBreakerService.snapshot(
                "email:" + properties.email().provider(),
                "sms:" + properties.sms().provider(),
                "whatsapp:" + properties.whatsapp().provider());
    }
}
