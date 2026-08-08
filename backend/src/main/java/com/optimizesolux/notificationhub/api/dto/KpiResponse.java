package com.optimizesolux.notificationhub.api.dto;

import com.optimizesolux.notificationhub.domain.Channel;

import java.util.List;
import java.util.Map;

public record KpiResponse(
        long windowHours,
        long total,
        long sent,
        long failed,
        long dead,
        long queued,
        double successRate,
        double avgAttempts,
        long outboxPending,
        List<ChannelCount> byChannel,
        List<StatusCount> byStatus,
        Map<String, Long> volumeLast24hByHour,
        List<ChannelCost> estimatedCostByChannel,
        double estimatedCostTotal
) {
    public record ChannelCount(Channel channel, long count) {}

    public record StatusCount(String status, long count) {}

    public record ChannelCost(Channel channel, long count, double unitCost, double estimatedCost) {}
}
