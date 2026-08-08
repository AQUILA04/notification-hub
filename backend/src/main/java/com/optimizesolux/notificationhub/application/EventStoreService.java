package com.optimizesolux.notificationhub.application;

import com.optimizesolux.notificationhub.domain.NotificationEventType;
import com.optimizesolux.notificationhub.infrastructure.persistence.NotificationEventEntity;
import com.optimizesolux.notificationhub.infrastructure.persistence.NotificationEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EventStoreService {

    private final NotificationEventRepository eventRepository;

    public EventStoreService(NotificationEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public NotificationEventEntity append(
            UUID notificationId,
            String tenantId,
            NotificationEventType type,
            Map<String, Object> payload) {
        int next = eventRepository.findMaxSequence(notificationId) + 1;
        NotificationEventEntity event = new NotificationEventEntity();
        event.setNotificationId(notificationId);
        event.setTenantId(tenantId);
        event.setSequenceNo(next);
        event.setEventType(type);
        event.setPayload(payload);
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<NotificationEventEntity> list(String tenantId, UUID notificationId) {
        return eventRepository.findByTenantIdAndNotificationIdOrderBySequenceNoAsc(
                tenantId, notificationId);
    }
}
