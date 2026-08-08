package com.optimizesolux.notificationhub.application;

import com.optimizesolux.notificationhub.api.dto.KpiResponse;
import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import com.optimizesolux.notificationhub.config.TenantContext;
import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.NotificationStatus;
import com.optimizesolux.notificationhub.infrastructure.persistence.NotificationRepository;
import com.optimizesolux.notificationhub.infrastructure.persistence.OutboxMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KpiService {

    private final NotificationRepository notificationRepository;
    private final OutboxMessageRepository outboxMessageRepository;
    private final NotificationHubProperties properties;

    public KpiService(
            NotificationRepository notificationRepository,
            OutboxMessageRepository outboxMessageRepository,
            NotificationHubProperties properties) {
        this.notificationRepository = notificationRepository;
        this.outboxMessageRepository = outboxMessageRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public KpiResponse snapshot(int windowHours) {
        String tenantId = TenantContext.require();
        int hours = Math.max(1, Math.min(windowHours, 168));
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);

        long total = notificationRepository.countSince(tenantId, since);
        long sent =
                notificationRepository.countByStatusSince(tenantId, NotificationStatus.SENT, since)
                        + notificationRepository.countByStatusSince(
                                tenantId, NotificationStatus.DELIVERED, since);
        long failed =
                notificationRepository.countByStatusSince(tenantId, NotificationStatus.FAILED, since);
        long dead =
                notificationRepository.countByStatusSince(tenantId, NotificationStatus.DEAD, since);
        long queued =
                notificationRepository.countByStatusSince(tenantId, NotificationStatus.QUEUED, since)
                        + notificationRepository.countByStatusSince(
                                tenantId, NotificationStatus.RECEIVED, since)
                        + notificationRepository.countByStatusSince(
                                tenantId, NotificationStatus.SENDING, since)
                        + notificationRepository.countByStatusSince(
                                tenantId, NotificationStatus.RENDERING, since);

        double successRate = total == 0 ? 0.0 : (double) sent / (double) total;
        Double avg = notificationRepository.avgAttemptsSince(tenantId, since);
        double avgAttempts = avg != null ? avg : 0.0;
        long outboxPending = outboxMessageRepository.countUnpublishedByTenant(tenantId);

        List<KpiResponse.ChannelCount> byChannel = new ArrayList<>();
        for (Object[] row : notificationRepository.countByChannelSince(tenantId, since)) {
            byChannel.add(new KpiResponse.ChannelCount((Channel) row[0], (Long) row[1]));
        }

        List<KpiResponse.StatusCount> byStatus = new ArrayList<>();
        for (Object[] row : notificationRepository.countByStatusGroupedSince(tenantId, since)) {
            byStatus.add(
                    new KpiResponse.StatusCount(((NotificationStatus) row[0]).name(), (Long) row[1]));
        }

        Map<String, Long> volumeByHour = new LinkedHashMap<>();
        for (Object[] row : notificationRepository.countGroupedByHourSince(tenantId, since)) {
            Instant bucket = toInstant(row[0]);
            long count = ((Number) row[1]).longValue();
            volumeByHour.put(bucket.toString(), count);
        }

        NotificationHubProperties.Cost cost = properties.cost();
        List<KpiResponse.ChannelCost> estimatedCostByChannel = new ArrayList<>();
        double estimatedCostTotal = 0.0;
        for (KpiResponse.ChannelCount row : byChannel) {
            double unit = unitCost(row.channel(), cost);
            double estimated = unit * row.count();
            estimatedCostTotal += estimated;
            estimatedCostByChannel.add(
                    new KpiResponse.ChannelCost(row.channel(), row.count(), unit, estimated));
        }

        return new KpiResponse(
                hours,
                total,
                sent,
                failed,
                dead,
                queued,
                successRate,
                avgAttempts,
                outboxPending,
                byChannel,
                byStatus,
                volumeByHour,
                estimatedCostByChannel,
                estimatedCostTotal);
    }

    private static double unitCost(Channel channel, NotificationHubProperties.Cost cost) {
        return switch (channel) {
            case EMAIL -> cost.email();
            case SMS -> cost.sms();
            case WHATSAPP -> cost.whatsapp();
        };
    }

    private static Instant toInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof java.time.OffsetDateTime odt) {
            return odt.toInstant();
        }
        return Instant.parse(value.toString());
    }
}
