package com.optimizesolux.notificationhub.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessageEntity, Long> {

    @Query(value = """
            SELECT * FROM outbox_messages
            WHERE published_at IS NULL
              AND available_at <= :now
            ORDER BY
              CASE priority
                WHEN 'HIGH' THEN 0
                WHEN 'NORMAL' THEN 1
                ELSE 2
              END,
              created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxMessageEntity> lockReady(@Param("now") Instant now, @Param("limit") int limit);

    @Query("""
            select count(o) from OutboxMessageEntity o
            where o.tenantId = :tenantId and o.publishedAt is null
            """)
    long countUnpublishedByTenant(String tenantId);
}
