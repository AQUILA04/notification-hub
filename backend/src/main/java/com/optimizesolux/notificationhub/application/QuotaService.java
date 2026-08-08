package com.optimizesolux.notificationhub.application;

import com.optimizesolux.notificationhub.api.QuotaExceededException;
import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import com.optimizesolux.notificationhub.domain.Channel;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Service
public class QuotaService {

    private static final DateTimeFormatter MINUTE =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC);

    private final StringRedisTemplate redis;
    private final NotificationHubProperties properties;

    public QuotaService(StringRedisTemplate redis, NotificationHubProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public void checkAndConsume(String tenantId, Channel channel, String appId) {
        NotificationHubProperties.Quota quota = properties.quota();
        if (quota == null || !quota.enabled()) {
            return;
        }
        String app = normalizeApp(appId);
        long limit = resolveLimit(channel, quota);
        if (limit <= 0) {
            return;
        }
        String bucket = MINUTE.format(Instant.now());
        String key = "nhub:rl:" + tenantId + ":" + channel.name() + ":" + app + ":" + bucket;
        Long current = redis.opsForValue().increment(key);
        if (current != null && current == 1L) {
            redis.expire(key, Duration.ofMinutes(2));
        }
        if (current != null && current > limit) {
            throw new QuotaExceededException(tenantId, channel.name(), app, limit, current);
        }
    }

    public static String resolveAppId(Map<String, Object> metadata, String headerAppId) {
        if (headerAppId != null && !headerAppId.isBlank()) {
            return headerAppId.trim();
        }
        if (metadata != null) {
            Object app = metadata.get("appId");
            if (app == null) {
                app = metadata.get("app");
            }
            if (app != null && !app.toString().isBlank()) {
                return app.toString().trim();
            }
        }
        return "default";
    }

    private long resolveLimit(Channel channel, NotificationHubProperties.Quota quota) {
        Map<String, Integer> perChannel = quota.perChannelPerMinute();
        if (perChannel != null) {
            Integer specific = perChannel.get(channel.name().toLowerCase(Locale.ROOT));
            if (specific == null) {
                specific = perChannel.get(channel.name());
            }
            if (specific != null) {
                return specific;
            }
        }
        return quota.defaultPerMinute();
    }

    private static String normalizeApp(String appId) {
        if (appId == null || appId.isBlank()) {
            return "default";
        }
        return appId.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }
}
