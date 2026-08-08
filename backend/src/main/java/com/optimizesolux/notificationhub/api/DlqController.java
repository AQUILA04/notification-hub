package com.optimizesolux.notificationhub.api;

import com.optimizesolux.notificationhub.api.dto.NotificationResponse;
import com.optimizesolux.notificationhub.api.dto.PageResponse;
import com.optimizesolux.notificationhub.application.DlqService;
import com.optimizesolux.notificationhub.domain.Channel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/dlq")
public class DlqController {

    private final DlqService dlqService;

    public DlqController(DlqService dlqService) {
        this.dlqService = dlqService;
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(
            @RequestParam(required = false) Channel channel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return dlqService.list(channel, page, size);
    }

    @PostMapping("/{id}/requeue")
    public NotificationResponse requeue(@PathVariable UUID id) {
        return dlqService.requeue(id);
    }

    @PostMapping("/{id}/discard")
    public NotificationResponse discard(@PathVariable UUID id) {
        return dlqService.discard(id);
    }
}
