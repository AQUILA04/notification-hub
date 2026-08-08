package com.optimizesolux.notificationhub.application;

import com.optimizesolux.notificationhub.api.NotFoundException;
import com.optimizesolux.notificationhub.api.dto.NotificationResponse;
import com.optimizesolux.notificationhub.api.dto.PageResponse;
import com.optimizesolux.notificationhub.config.TenantContext;
import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.NotificationEventType;
import com.optimizesolux.notificationhub.domain.NotificationStatus;
import com.optimizesolux.notificationhub.infrastructure.persistence.NotificationEntity;
import com.optimizesolux.notificationhub.infrastructure.persistence.NotificationRepository;
import com.optimizesolux.notificationhub.infrastructure.persistence.OutboxMessageEntity;
import com.optimizesolux.notificationhub.infrastructure.persistence.OutboxMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class DlqService {

    private final NotificationRepository notificationRepository;
    private final OutboxMessageRepository outboxMessageRepository;
    private final EventStoreService eventStoreService;
    private final ChannelAvailabilityService channelAvailabilityService;
    private final AuditService auditService;

    public DlqService(
            NotificationRepository notificationRepository,
            OutboxMessageRepository outboxMessageRepository,
            EventStoreService eventStoreService,
            ChannelAvailabilityService channelAvailabilityService,
            AuditService auditService) {
        this.notificationRepository = notificationRepository;
        this.outboxMessageRepository = outboxMessageRepository;
        this.eventStoreService = eventStoreService;
        this.channelAvailabilityService = channelAvailabilityService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Channel channel, int page, int size) {
        String tenantId = TenantContext.require();
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);
        Page<NotificationEntity> result =
                notificationRepository.search(
                        tenantId, NotificationStatus.DEAD, channel, PageRequest.of(safePage, safeSize));
        return new PageResponse<>(
                result.getContent().stream().map(NotificationService::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional
    public NotificationResponse requeue(UUID id) {
        String tenantId = TenantContext.require();
        NotificationEntity entity = requireDead(tenantId, id);
        channelAvailabilityService.requireEnabled(entity.getChannel());

        entity.setStatus(NotificationStatus.QUEUED);
        entity.setAttemptCount(0);
        entity.setLastError(null);
        entity.setUpdatedAt(Instant.now());
        notificationRepository.save(entity);

        OutboxMessageEntity outbox = new OutboxMessageEntity();
        outbox.setNotificationId(entity.getId());
        outbox.setTenantId(tenantId);
        outbox.setChannel(entity.getChannel());
        outbox.setPriority(entity.getPriority());
        outbox.setAvailableAt(Instant.now());
        Map<String, Object> payload = new HashMap<>();
        payload.put("notificationId", entity.getId().toString());
        payload.put("tenantId", tenantId);
        payload.put("channel", entity.getChannel().name());
        payload.put("dlqRequeue", true);
        outbox.setPayload(payload);
        outboxMessageRepository.save(outbox);

        eventStoreService.append(
                entity.getId(),
                tenantId,
                NotificationEventType.QUEUED,
                Map.of("dlqRequeue", true));
        auditService.record(tenantId, "DLQ_REQUEUE", entity.getId().toString(), Map.of("channel", entity.getChannel().name()));

        return NotificationService.toResponse(entity);
    }

    @Transactional
    public NotificationResponse discard(UUID id) {
        String tenantId = TenantContext.require();
        NotificationEntity entity = requireDead(tenantId, id);
        entity.setStatus(NotificationStatus.CANCELLED);
        entity.setUpdatedAt(Instant.now());
        notificationRepository.save(entity);
        eventStoreService.append(
                entity.getId(),
                tenantId,
                NotificationEventType.CANCELLED,
                Map.of("dlqDiscard", true));
        auditService.record(
                tenantId, "DLQ_DISCARD", entity.getId().toString(), Map.of("channel", entity.getChannel().name()));
        return NotificationService.toResponse(entity);
    }

    private NotificationEntity requireDead(String tenantId, UUID id) {
        NotificationEntity entity =
                notificationRepository
                        .findByTenantIdAndId(tenantId, id)
                        .orElseThrow(() -> new NotFoundException("Notification not found: " + id));
        if (entity.getStatus() != NotificationStatus.DEAD) {
            throw new IllegalStateException("Notification is not in DLQ (DEAD): " + entity.getStatus());
        }
        return entity;
    }
}
