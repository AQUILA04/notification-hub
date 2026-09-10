package com.optimizesolux.notificationhub.application;

import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.NotificationEnvironment;
import com.optimizesolux.notificationhub.domain.NotificationEventType;
import com.optimizesolux.notificationhub.domain.NotificationStatus;
import com.optimizesolux.notificationhub.infrastructure.messaging.DispatchMessage;
import com.optimizesolux.notificationhub.infrastructure.persistence.NotificationEntity;
import com.optimizesolux.notificationhub.infrastructure.persistence.NotificationRepository;
import com.optimizesolux.notificationhub.infrastructure.persistence.OutboxMessageEntity;
import com.optimizesolux.notificationhub.infrastructure.persistence.OutboxMessageRepository;
import com.optimizesolux.notificationhub.infrastructure.persistence.TemplateEntity;
import com.optimizesolux.notificationhub.infrastructure.provider.ChannelSenderRegistry;
import com.optimizesolux.notificationhub.infrastructure.provider.MailpitSmsBridge;
import com.optimizesolux.notificationhub.infrastructure.provider.WhatsAppChannelSender;
import com.optimizesolux.notificationhub.infrastructure.template.PebbleTemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final NotificationRepository notificationRepository;
    private final OutboxMessageRepository outboxMessageRepository;
    private final EventStoreService eventStoreService;
    private final TemplateService templateService;
    private final PebbleTemplateRenderer renderer;
    private final ChannelSenderRegistry channelSenderRegistry;
    private final MailpitSmsBridge mailpitSmsBridge;
    private final NotificationHubProperties properties;
    private final CircuitBreakerService circuitBreakerService;

    public NotificationDispatchService(
            NotificationRepository notificationRepository,
            OutboxMessageRepository outboxMessageRepository,
            EventStoreService eventStoreService,
            TemplateService templateService,
            PebbleTemplateRenderer renderer,
            ChannelSenderRegistry channelSenderRegistry,
            MailpitSmsBridge mailpitSmsBridge,
            NotificationHubProperties properties,
            CircuitBreakerService circuitBreakerService) {
        this.notificationRepository = notificationRepository;
        this.outboxMessageRepository = outboxMessageRepository;
        this.eventStoreService = eventStoreService;
        this.templateService = templateService;
        this.renderer = renderer;
        this.channelSenderRegistry = channelSenderRegistry;
        this.mailpitSmsBridge = mailpitSmsBridge;
        this.properties = properties;
        this.circuitBreakerService = circuitBreakerService;
    }

    @JmsListener(destination = "${notification-hub.queues.email}")
    @Transactional
    public void onEmail(DispatchMessage message) {
        dispatch(message);
    }

    @JmsListener(destination = "${notification-hub.queues.sms}")
    @Transactional
    public void onSms(DispatchMessage message) {
        dispatch(message);
    }

    @JmsListener(destination = "${notification-hub.queues.whatsapp}")
    @Transactional
    public void onWhatsapp(DispatchMessage message) {
        dispatch(message);
    }

    void dispatch(DispatchMessage message) {
        UUID id = UUID.fromString(message.notificationId());
        NotificationEntity entity =
                notificationRepository
                        .findByTenantIdAndId(message.tenantId(), id)
                        .orElse(null);
        if (entity == null) {
            log.warn("Notification missing for dispatch {}", message.notificationId());
            return;
        }
        if (entity.getStatus() == NotificationStatus.SENT
                || entity.getStatus() == NotificationStatus.DEAD
                || entity.getStatus() == NotificationStatus.CANCELLED) {
            return;
        }

        entity.setAttemptCount(entity.getAttemptCount() + 1);
        entity.setStatus(NotificationStatus.SENDING);
        entity.setUpdatedAt(Instant.now());
        notificationRepository.save(entity);

        eventStoreService.append(
                entity.getId(),
                entity.getTenantId(),
                NotificationEventType.ATTEMPT_STARTED,
                Map.of("attempt", entity.getAttemptCount()));

        try {
            String subject = entity.getSubject();
            String body = entity.getBody();
            Map<String, Object> templateData =
                    entity.getTemplateData() != null ? entity.getTemplateData() : Map.of();

            if (entity.getChannel() == Channel.WHATSAPP
                    && entity.getTemplateName() != null
                    && entity.getTemplateName().startsWith("HX")) {
                // Twilio Content Template SID — pas de rendu Pebble
                body = entity.getTemplateName();
            } else if (entity.getTemplateName() != null) {
                entity.setStatus(NotificationStatus.RENDERING);
                notificationRepository.save(entity);
                TemplateEntity template =
                        templateService.requireActive(entity.getTenantId(), entity.getTemplateName());
                Map<String, Object> data = templateData;
                if (template.getSubjectTemplate() != null) {
                    subject = renderer.render(template.getSubjectTemplate(), data);
                }
                body = renderer.render(template.getBodyTemplate(), data);
                eventStoreService.append(
                        entity.getId(),
                        entity.getTenantId(),
                        NotificationEventType.RENDERED,
                        Map.of("template", template.getName(), "version", template.getVersion()));
                entity.setStatus(NotificationStatus.SENDING);
                notificationRepository.save(entity);
            }

            String circuitKey = circuitKey(entity);
            circuitBreakerService.beforeCall(circuitKey);
            String providerId;
            try {
                providerId =
                        send(
                                entity,
                                entity.getFromAddress(),
                                entity.getToAddresses(),
                                subject,
                                body,
                                templateData);
                circuitBreakerService.onSuccess(circuitKey);
            } catch (Exception sendEx) {
                circuitBreakerService.onFailure(circuitKey);
                throw sendEx;
            }

            entity.setStatus(NotificationStatus.SENT);
            entity.setProviderMessageId(providerId);
            entity.setSentAt(Instant.now());
            entity.setLastError(null);
            entity.setUpdatedAt(Instant.now());
            notificationRepository.save(entity);

            Map<String, Object> successPayload = new HashMap<>();
            successPayload.put("providerMessageId", providerId);
            if (isSmsTestIntercept(entity)) {
                successPayload.put("intercepted", true);
                successPayload.put("via", "mailpit");
            }
            eventStoreService.append(
                    entity.getId(),
                    entity.getTenantId(),
                    NotificationEventType.ATTEMPT_SUCCEEDED,
                    successPayload);
        } catch (Exception ex) {
            log.error("Dispatch failed for {}: {}", entity.getId(), ex.getMessage());
            entity.setLastError(ex.getMessage());
            entity.setUpdatedAt(Instant.now());

            if (entity.getAttemptCount() >= entity.getMaxAttempts()) {
                entity.setStatus(NotificationStatus.DEAD);
                notificationRepository.save(entity);
                eventStoreService.append(
                        entity.getId(),
                        entity.getTenantId(),
                        NotificationEventType.DEAD_LETTERED,
                        Map.of("error", String.valueOf(ex.getMessage())));
            } else {
                entity.setStatus(NotificationStatus.FAILED);
                notificationRepository.save(entity);
                eventStoreService.append(
                        entity.getId(),
                        entity.getTenantId(),
                        NotificationEventType.ATTEMPT_FAILED,
                        Map.of(
                                "error",
                                String.valueOf(ex.getMessage()),
                                "attempt",
                                entity.getAttemptCount()));
                scheduleRetry(entity);
            }
        }
    }

    private String send(
            NotificationEntity entity,
            String from,
            java.util.List<String> to,
            String subject,
            String body,
            Map<String, Object> templateData)
            throws Exception {
        if (isSmsTestIntercept(entity)) {
            return mailpitSmsBridge.send(from, to, body);
        }
        var sender = channelSenderRegistry.require(entity.getChannel());
        if (sender instanceof WhatsAppChannelSender whatsApp) {
            return whatsApp.sendWithTemplateData(from, to, body, templateData);
        }
        return sender.send(from, to, subject, body);
    }

    private void scheduleRetry(NotificationEntity entity) {
        long delay =
                computeDelayMs(
                        entity.getAttemptCount(),
                        properties.retry().initialDelayMs(),
                        properties.retry().multiplier(),
                        properties.retry().maxDelayMs());

        OutboxMessageEntity outbox = new OutboxMessageEntity();
        outbox.setNotificationId(entity.getId());
        outbox.setTenantId(entity.getTenantId());
        outbox.setChannel(entity.getChannel());
        outbox.setPriority(entity.getPriority());
        Instant availableAt = Instant.now().plusMillis(delay);
        outbox.setAvailableAt(availableAt);
        Map<String, Object> payload = new HashMap<>();
        payload.put("notificationId", entity.getId().toString());
        payload.put("tenantId", entity.getTenantId());
        payload.put("channel", entity.getChannel().name());
        payload.put("retryAfterMs", delay);
        payload.put("availableAt", availableAt.toString());
        outbox.setPayload(payload);
        outboxMessageRepository.save(outbox);
        log.info(
                "Retry scheduled notification={} attempt={} delayMs={} availableAt={}",
                entity.getId(),
                entity.getAttemptCount(),
                delay,
                availableAt);
    }

    static long computeDelayMs(int attempt, long initial, double multiplier, long max) {
        double raw = initial * Math.pow(multiplier, Math.max(0, attempt - 1));
        return Math.min(max, (long) raw);
    }

    private String circuitKey(NotificationEntity entity) {
        if (isSmsTestIntercept(entity)) {
            String emailProvider =
                    properties.email() != null && properties.email().provider() != null
                            ? properties.email().provider()
                            : "smtp";
            return "sms:mailpit:" + emailProvider;
        }
        Channel channel = entity.getChannel();
        String provider =
                switch (channel) {
                    case EMAIL -> properties.email().provider();
                    case SMS -> properties.sms().provider();
                    case WHATSAPP -> properties.whatsapp().provider();
                };
        return channel.name().toLowerCase() + ":" + provider;
    }

    static boolean isSmsTestIntercept(NotificationEntity entity) {
        return entity.getChannel() == Channel.SMS
                && !NotificationEnvironment.from(entity.getEnvironment()).isProd();
    }
}
