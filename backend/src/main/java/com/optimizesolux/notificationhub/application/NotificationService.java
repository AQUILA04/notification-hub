package com.optimizesolux.notificationhub.application;

import com.optimizesolux.notificationhub.api.NotFoundException;
import com.optimizesolux.notificationhub.api.dto.CreateNotificationRequest;
import com.optimizesolux.notificationhub.api.dto.NotificationEventResponse;
import com.optimizesolux.notificationhub.api.dto.NotificationResponse;
import com.optimizesolux.notificationhub.api.dto.PageResponse;
import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import com.optimizesolux.notificationhub.config.TenantContext;
import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.NotificationEventType;
import com.optimizesolux.notificationhub.domain.NotificationStatus;
import com.optimizesolux.notificationhub.domain.Priority;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final OutboxMessageRepository outboxMessageRepository;
    private final EventStoreService eventStoreService;
    private final NotificationHubProperties properties;
    private final ChannelAvailabilityService channelAvailabilityService;
    private final QuotaService quotaService;
    private final AuditService auditService;

    public NotificationService(
            NotificationRepository notificationRepository,
            OutboxMessageRepository outboxMessageRepository,
            EventStoreService eventStoreService,
            NotificationHubProperties properties,
            ChannelAvailabilityService channelAvailabilityService,
            QuotaService quotaService,
            AuditService auditService) {
        this.notificationRepository = notificationRepository;
        this.outboxMessageRepository = outboxMessageRepository;
        this.eventStoreService = eventStoreService;
        this.properties = properties;
        this.channelAvailabilityService = channelAvailabilityService;
        this.quotaService = quotaService;
        this.auditService = auditService;
    }

    @Transactional
    public NotificationResponse create(
            CreateNotificationRequest request, String idempotencyKey, String appIdHeader) {
        String tenantId = TenantContext.require();
        channelAvailabilityService.requireEnabled(request.channel());
        validateContent(request);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing =
                    notificationRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        String appId = QuotaService.resolveAppId(request.metadata(), appIdHeader);
        quotaService.checkAndConsume(tenantId, request.channel(), appId);

        Priority priority = request.priority() != null ? request.priority() : Priority.NORMAL;
        int maxAttempts =
                request.retryPolicy() != null && request.retryPolicy().maxAttempts() != null
                        ? request.retryPolicy().maxAttempts()
                        : properties.retry().defaultMaxAttempts();

        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        NotificationEntity entity = new NotificationEntity();
        entity.setId(id);
        entity.setTenantId(tenantId);
        entity.setChannel(request.channel());
        entity.setStatus(NotificationStatus.RECEIVED);
        entity.setFromAddress(request.from());
        entity.setToAddresses(request.to());
        entity.setSubject(request.subject());
        entity.setBody(request.body());
        entity.setTemplateName(request.templateName());
        entity.setTemplateData(request.templateData());
        entity.setPriority(priority);
        entity.setMaxAttempts(maxAttempts);
        entity.setIdempotencyKey(idempotencyKey);
        entity.setMetadata(request.metadata());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        notificationRepository.save(entity);

        eventStoreService.append(
                id,
                tenantId,
                NotificationEventType.RECEIVED,
                Map.of(
                        "channel", request.channel().name(),
                        "from", request.from(),
                        "to", request.to()));

        OutboxMessageEntity outbox = new OutboxMessageEntity();
        outbox.setNotificationId(id);
        outbox.setTenantId(tenantId);
        outbox.setChannel(request.channel());
        outbox.setPriority(priority);
        Map<String, Object> payload = new HashMap<>();
        payload.put("notificationId", id.toString());
        payload.put("tenantId", tenantId);
        payload.put("channel", request.channel().name());
        outbox.setPayload(payload);
        outboxMessageRepository.save(outbox);

        entity.setStatus(NotificationStatus.QUEUED);
        entity.setUpdatedAt(Instant.now());
        notificationRepository.save(entity);

        eventStoreService.append(
                id, tenantId, NotificationEventType.QUEUED, Map.of("outbox", true));

        auditService.record(
                tenantId,
                "NOTIFICATION_CREATE",
                id.toString(),
                Map.of("channel", request.channel().name(), "appId", appId));

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public NotificationResponse get(UUID id) {
        String tenantId = TenantContext.require();
        return toResponse(require(tenantId, id));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(
            NotificationStatus status, Channel channel, int page, int size) {
        String tenantId = TenantContext.require();
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);
        Page<NotificationEntity> result =
                notificationRepository.search(
                        tenantId, status, channel, PageRequest.of(safePage, safeSize));
        return new PageResponse<>(
                result.getContent().stream().map(NotificationService::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<NotificationEventResponse> events(UUID id) {
        String tenantId = TenantContext.require();
        require(tenantId, id);
        return eventStoreService.list(tenantId, id).stream()
                .map(
                        e ->
                                new NotificationEventResponse(
                                        e.getId(),
                                        e.getSequenceNo(),
                                        e.getEventType(),
                                        e.getPayload(),
                                        e.getOccurredAt()))
                .toList();
    }

    private NotificationEntity require(String tenantId, UUID id) {
        return notificationRepository
                .findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + id));
    }

    private void validateContent(CreateNotificationRequest request) {
        boolean hasBody = request.body() != null && !request.body().isBlank();
        boolean hasTemplate = request.templateName() != null && !request.templateName().isBlank();
        if (!hasBody && !hasTemplate) {
            throw new IllegalArgumentException("Provide either body or templateName + templateData");
        }
        if (hasBody && hasTemplate) {
            throw new IllegalArgumentException("Provide either body or templateName, not both");
        }
        if (request.channel() == Channel.EMAIL
                && (request.subject() == null || request.subject().isBlank())
                && !hasTemplate) {
            throw new IllegalArgumentException("Email requires subject when sending plain body");
        }
        if (request.channel() == Channel.WHATSAPP && hasTemplate) {
            String name = request.templateName();
            // ContentSid Twilio (HX…) OK without Pebble template row
            if (name != null && !name.startsWith("HX") && request.templateData() == null) {
                throw new IllegalArgumentException(
                        "WhatsApp template requires templateData, or use a Twilio ContentSid (HX…)");
            }
        }
    }

    static NotificationResponse toResponse(NotificationEntity e) {
        return new NotificationResponse(
                e.getId(),
                e.getTenantId(),
                e.getChannel(),
                e.getStatus(),
                e.getFromAddress(),
                e.getToAddresses(),
                e.getSubject(),
                e.getTemplateName(),
                e.getPriority(),
                e.getAttemptCount(),
                e.getMaxAttempts(),
                e.getLastError(),
                e.getMetadata(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getSentAt());
    }
}
