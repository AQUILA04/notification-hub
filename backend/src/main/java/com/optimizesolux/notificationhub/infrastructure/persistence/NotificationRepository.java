package com.optimizesolux.notificationhub.infrastructure.persistence;

import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    Optional<NotificationEntity> findByTenantIdAndId(String tenantId, UUID id);

    Optional<NotificationEntity> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);

    Optional<NotificationEntity> findByProviderMessageId(String providerMessageId);

    @Query("""
            select n from NotificationEntity n
            where n.tenantId = :tenantId
              and (:status is null or n.status = :status)
              and (:channel is null or n.channel = :channel)
            order by n.createdAt desc
            """)
    Page<NotificationEntity> search(
            @Param("tenantId") String tenantId,
            @Param("status") NotificationStatus status,
            @Param("channel") Channel channel,
            Pageable pageable);

    @Query("""
            select count(n) from NotificationEntity n
            where n.tenantId = :tenantId and n.createdAt >= :since
            """)
    long countSince(@Param("tenantId") String tenantId, @Param("since") Instant since);

    @Query("""
            select count(n) from NotificationEntity n
            where n.tenantId = :tenantId and n.status = :status and n.createdAt >= :since
            """)
    long countByStatusSince(
            @Param("tenantId") String tenantId,
            @Param("status") NotificationStatus status,
            @Param("since") Instant since);

    @Query("""
            select n.channel, count(n) from NotificationEntity n
            where n.tenantId = :tenantId and n.createdAt >= :since
            group by n.channel
            """)
    List<Object[]> countByChannelSince(
            @Param("tenantId") String tenantId, @Param("since") Instant since);

    @Query("""
            select n.status, count(n) from NotificationEntity n
            where n.tenantId = :tenantId and n.createdAt >= :since
            group by n.status
            """)
    List<Object[]> countByStatusGroupedSince(
            @Param("tenantId") String tenantId, @Param("since") Instant since);

    @Query("""
            select coalesce(avg(n.attemptCount), 0) from NotificationEntity n
            where n.tenantId = :tenantId and n.createdAt >= :since
            """)
    Double avgAttemptsSince(@Param("tenantId") String tenantId, @Param("since") Instant since);

    @Query(value = """
            SELECT date_trunc('hour', created_at) AS hour_bucket, COUNT(*) AS cnt
            FROM notifications
            WHERE tenant_id = :tenantId AND created_at >= :since
            GROUP BY hour_bucket
            ORDER BY hour_bucket
            """, nativeQuery = true)
    List<Object[]> countGroupedByHourSince(
            @Param("tenantId") String tenantId, @Param("since") Instant since);
}
