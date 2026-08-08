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
}
