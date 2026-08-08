package com.optimizesolux.notificationhub.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface NotificationEventRepository extends JpaRepository<NotificationEventEntity, Long> {

    List<NotificationEventEntity> findByTenantIdAndNotificationIdOrderBySequenceNoAsc(
            String tenantId, UUID notificationId);

    @Query("""
            select coalesce(max(e.sequenceNo), 0) from NotificationEventEntity e
            where e.notificationId = :notificationId
            """)
    int findMaxSequence(UUID notificationId);
}
