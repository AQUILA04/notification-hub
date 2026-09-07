package com.optimizesolux.notificationhub.application;

import com.optimizesolux.notificationhub.domain.NotificationEventType;
import com.optimizesolux.notificationhub.domain.NotificationStatus;
import com.optimizesolux.notificationhub.infrastructure.persistence.NotificationEntity;
import com.optimizesolux.notificationhub.infrastructure.persistence.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class ProviderWebhookService {

    private static final Logger log = LoggerFactory.getLogger(ProviderWebhookService.class);

    private final NotificationRepository notificationRepository;
    private final EventStoreService eventStoreService;

    public ProviderWebhookService(
            NotificationRepository notificationRepository, EventStoreService eventStoreService) {
        this.notificationRepository = notificationRepository;
        this.eventStoreService = eventStoreService;
    }

    @Transactional
    public void handleTwilioStatus(String messageSid, String messageStatus, String errorMessage) {
        if (messageSid == null || messageSid.isBlank()) {
            return;
        }
        Optional<NotificationEntity> opt = notificationRepository.findByProviderMessageId(messageSid);
        if (opt.isEmpty()) {
            log.debug("Twilio webhook unknown MessageSid={}", messageSid);
            return;
        }
        NotificationEntity entity = opt.get();
        String status = messageStatus != null ? messageStatus.toLowerCase(Locale.ROOT) : "";

        NotificationStatus mapped =
                switch (status) {
                    case "delivered", "read" -> NotificationStatus.DELIVERED;
                    case "failed", "undelivered" -> NotificationStatus.FAILED;
                    case "sent" -> NotificationStatus.SENT;
                    default -> null;
                };

        eventStoreService.append(
                entity.getId(),
                entity.getTenantId(),
                NotificationEventType.PROVIDER_ACK,
                Map.of(
                        "provider",
                        "twilio",
                        "messageStatus",
                        String.valueOf(messageStatus),
                        "error",
                        errorMessage != null ? errorMessage : ""));

        if (mapped != null) {
            entity.setStatus(mapped);
            if (errorMessage != null && !errorMessage.isBlank()) {
                entity.setLastError(errorMessage);
            }
            entity.setUpdatedAt(Instant.now());
            notificationRepository.save(entity);
        }
    }

    @Transactional
    public void handleBrevoSmsStatus(String messageId, String msgStatus, String description) {
        if (messageId == null || messageId.isBlank()) {
            return;
        }
        String providerMessageId =
                messageId.startsWith("brevo-") ? messageId : "brevo-" + messageId;
        Optional<NotificationEntity> opt =
                notificationRepository.findByProviderMessageId(providerMessageId);
        if (opt.isEmpty()) {
            // fallback: raw id if stored without prefix
            opt = notificationRepository.findByProviderMessageId(messageId);
        }
        if (opt.isEmpty()) {
            log.debug("Brevo SMS webhook unknown messageId={}", messageId);
            return;
        }
        NotificationEntity entity = opt.get();
        String status = msgStatus != null ? msgStatus.toLowerCase(Locale.ROOT) : "";

        NotificationStatus mapped =
                switch (status) {
                    case "delivered" -> NotificationStatus.DELIVERED;
                    case "hard_bounce", "soft_bounce", "rejected", "failed" -> NotificationStatus.FAILED;
                    case "sent", "accepted" -> NotificationStatus.SENT;
                    default -> null;
                };

        eventStoreService.append(
                entity.getId(),
                entity.getTenantId(),
                NotificationEventType.PROVIDER_ACK,
                Map.of(
                        "provider",
                        "brevo",
                        "messageStatus",
                        String.valueOf(msgStatus),
                        "error",
                        description != null ? description : ""));

        if (mapped != null) {
            entity.setStatus(mapped);
            if (description != null
                    && !description.isBlank()
                    && mapped == NotificationStatus.FAILED) {
                entity.setLastError(description);
            }
            entity.setUpdatedAt(Instant.now());
            notificationRepository.save(entity);
        }
    }
}
