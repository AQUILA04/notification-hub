package com.optimizesolux.notificationhub.api;

import com.optimizesolux.notificationhub.api.dto.CreateNotificationRequest;
import com.optimizesolux.notificationhub.api.dto.NotificationEventResponse;
import com.optimizesolux.notificationhub.api.dto.NotificationResponse;
import com.optimizesolux.notificationhub.api.dto.PageResponse;
import com.optimizesolux.notificationhub.api.dto.ReplayNotificationRequest;
import com.optimizesolux.notificationhub.application.NotificationService;
import com.optimizesolux.notificationhub.application.ReplayService;
import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.NotificationStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final ReplayService replayService;

    public NotificationController(
            NotificationService notificationService, ReplayService replayService) {
        this.notificationService = notificationService;
        this.replayService = replayService;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> create(
            @Valid @RequestBody CreateNotificationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-App-Id", required = false) String appId) {
        NotificationResponse created = notificationService.create(request, idempotencyKey, appId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(created);
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) Channel channel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return notificationService.list(status, channel, page, size);
    }

    @GetMapping("/{id}")
    public NotificationResponse get(@PathVariable UUID id) {
        return notificationService.get(id);
    }

    @GetMapping("/{id}/events")
    public List<NotificationEventResponse> events(@PathVariable UUID id) {
        return notificationService.events(id);
    }

    @PostMapping("/{id}/replay")
    public Map<String, Object> replay(
            @PathVariable UUID id, @Valid @RequestBody ReplayNotificationRequest request) {
        return replayService.replay(id, request);
    }
}
