package com.optimizesolux.notificationhub.application;

import com.optimizesolux.notificationhub.api.NotFoundException;
import com.optimizesolux.notificationhub.api.dto.NotificationResponse;
import com.optimizesolux.notificationhub.api.dto.ReplayNotificationRequest;
import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import com.optimizesolux.notificationhub.config.TenantContext;
import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.NotificationEventType;
import com.optimizesolux.notificationhub.domain.NotificationStatus;
import com.optimizesolux.notificationhub.infrastructure.persistence.NotificationEntity;
import com.optimizesolux.notificationhub.infrastructure.persistence.NotificationRepository;
import com.optimizesolux.notificationhub.infrastructure.persistence.OutboxMessageEntity;
import com.optimizesolux.notificationhub.infrastructure.persistence.OutboxMessageRepository;
import com.optimizesolux.notificationhub.infrastructure.persistence.TemplateEntity;
import com.optimizesolux.notificationhub.infrastructure.template.PebbleTemplateRenderer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ReplayService {

    private final NotificationRepository notificationRepository;
    private final OutboxMessageRepository outboxMessageRepository;
    private final EventStoreService eventStoreService;
    private final TemplateService templateService;
    private final PebbleTemplateRenderer renderer;
    private final ChannelAvailabilityService channelAvailabilityService;
    private final AuditService auditService;
    private final NotificationHubProperties properties;

    public ReplayService(
            NotificationRepository notificationRepository,
            OutboxMessageRepository outboxMessageRepository,
            EventStoreService eventStoreService,
            TemplateService templateService,
            PebbleTemplateRenderer renderer,
            ChannelAvailabilityService channelAvailabilityService,
            AuditService auditService,
            NotificationHubProperties properties) {
        this.notificationRepository = notificationRepository;
        this.outboxMessageRepository = outboxMessageRepository;
        this.eventStoreService = eventStoreService;
        this.templateService = templateService;
        this.renderer = renderer;
        this.channelAvailabilityService = channelAvailabilityService;
        this.auditService = auditService;
        this.properties = properties;
    }

    @Transactional
    public Map<String, Object> replay(UUID id, ReplayNotificationRequest request) {
        String tenantId = TenantContext.require();
        NotificationEntity entity =
                notificationRepository
                        .findByTenantIdAndId(tenantId, id)
                        .orElseThrow(() -> new NotFoundException("Notification not found: " + id));

        channelAvailabilityService.requireEnabled(entity.getChannel());

        eventStoreService.append(
                entity.getId(),
                tenantId,
                NotificationEventType.REPLAY_REQUESTED,
                Map.of("mode", request.mode().name()));

        if (request.mode() == ReplayNotificationRequest.ReplayMode.DRY_RUN) {
            Map<String, Object> result = new HashMap<>();
            result.put("mode", "DRY_RUN");
            result.put("notificationId", id.toString());
            result.put("channel", entity.getChannel().name());
            if (entity.getTemplateName() != null
                    && !entity.getTemplateName().startsWith("HX")
                    && !(entity.getChannel() == Channel.WHATSAPP
                            && OtpRequestResolver.isMetaProvider(properties))) {
                TemplateEntity template =
                        templateService.requireActive(tenantId, entity.getTemplateName());
                Map<String, Object> data =
                        entity.getTemplateData() != null ? entity.getTemplateData() : Map.of();
                String subject =
                        template.getSubjectTemplate() != null
                                ? renderer.render(template.getSubjectTemplate(), data)
                                : entity.getSubject();
                String body = renderer.render(template.getBodyTemplate(), data);
                result.put("subject", subject);
                result.put("body", body);
            } else {
                result.put("subject", entity.getSubject());
                result.put("body", entity.getBody());
                result.put("templateName", entity.getTemplateName());
            }
            return result;
        }

        // RESEND
        entity.setStatus(NotificationStatus.QUEUED);
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
        payload.put("replay", true);
        outbox.setPayload(payload);
        outboxMessageRepository.save(outbox);

        eventStoreService.append(
                entity.getId(), tenantId, NotificationEventType.QUEUED, Map.of("replay", true));

        auditService.record(
                tenantId,
                "NOTIFICATION_REPLAY",
                entity.getId().toString(),
                Map.of("mode", "RESEND", "channel", entity.getChannel().name()));

        return Map.of(
                "mode",
                "RESEND",
                "notification",
                NotificationService.toResponse(entity));
    }
}
