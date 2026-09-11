package com.optimizesolux.notificationhub.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public ProviderWebhookService(
            NotificationRepository notificationRepository,
            EventStoreService eventStoreService,
            ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.eventStoreService = eventStoreService;
        this.objectMapper = objectMapper;
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

    /**
     * Parse Meta Cloud API webhook JSON (statuses + inbound messages). Updates notification status
     * by {@code wamid} stored as {@code providerMessageId}.
     */
    @Transactional
    public void handleMetaWebhook(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode entries = root.path("entry");
            if (!entries.isArray()) {
                return;
            }
            for (JsonNode entry : entries) {
                JsonNode changes = entry.path("changes");
                if (!changes.isArray()) {
                    continue;
                }
                for (JsonNode change : changes) {
                    JsonNode value = change.path("value");
                    applyMetaStatuses(value.path("statuses"));
                    JsonNode inbound = value.path("messages");
                    if (inbound.isArray() && !inbound.isEmpty()) {
                        log.info(
                                "Meta WhatsApp inbound messages count={} (not processed)",
                                inbound.size());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Meta webhook parse error: {}", e.getMessage());
        }
    }

    private void applyMetaStatuses(JsonNode statuses) {
        if (!statuses.isArray()) {
            return;
        }
        for (JsonNode statusNode : statuses) {
            String wamid = textOrNull(statusNode, "id");
            String status = textOrNull(statusNode, "status");
            String errorMessage = extractMetaError(statusNode);
            handleMetaStatus(wamid, status, errorMessage);
        }
    }

    private void handleMetaStatus(String wamid, String messageStatus, String errorMessage) {
        if (wamid == null || wamid.isBlank()) {
            return;
        }
        Optional<NotificationEntity> opt = notificationRepository.findByProviderMessageId(wamid);
        if (opt.isEmpty()) {
            log.debug("Meta webhook unknown wamid={}", wamid);
            return;
        }
        NotificationEntity entity = opt.get();
        String status = messageStatus != null ? messageStatus.toLowerCase(Locale.ROOT) : "";

        NotificationStatus mapped =
                switch (status) {
                    case "delivered", "read" -> NotificationStatus.DELIVERED;
                    case "failed" -> NotificationStatus.FAILED;
                    case "sent" -> NotificationStatus.SENT;
                    default -> null;
                };

        eventStoreService.append(
                entity.getId(),
                entity.getTenantId(),
                NotificationEventType.PROVIDER_ACK,
                Map.of(
                        "provider",
                        "meta",
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

    private static String extractMetaError(JsonNode statusNode) {
        JsonNode errors = statusNode.path("errors");
        if (!errors.isArray() || errors.isEmpty()) {
            return null;
        }
        JsonNode first = errors.get(0);
        String title = textOrNull(first, "title");
        String message = textOrNull(first, "message");
        if (title != null && message != null) {
            return title + ": " + message;
        }
        return message != null ? message : title;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) {
            return null;
        }
        String s = v.asText();
        return s != null && !s.isBlank() ? s : null;
    }
}
