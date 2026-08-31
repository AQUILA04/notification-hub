package com.optimizesolux.notificationhub.otp.infrastructure;

import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.otp.api.OtpResendCooldownException;
import com.optimizesolux.notificationhub.otp.domain.OtpSession;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RedisOtpStore implements OtpStore {

    private final StringRedisTemplate redis;

    public RedisOtpStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void save(OtpSession session, Duration ttl) {
        String destKey = destinationKey(session.tenantId(), session.channel(), session.destination());
        String sessionKey = sessionKey(session.tenantId(), session.sessionId());

        String destPayload =
                session.sessionId()
                        + "|"
                        + session.codeHash()
                        + "|"
                        + session.expiresAt().toEpochMilli();

        String sessionPayload =
                session.channel().name()
                        + "|"
                        + session.destination()
                        + "|"
                        + session.codeHash()
                        + "|"
                        + session.expiresAt().toEpochMilli();

        redis.opsForValue().set(destKey, destPayload, ttl);
        redis.opsForValue().set(sessionKey, sessionPayload, ttl);
    }

    @Override
    public Optional<OtpSession> findBySessionId(String tenantId, UUID sessionId) {
        return parseSession(tenantId, sessionId, redis.opsForValue().get(sessionKey(tenantId, sessionId)));
    }

    @Override
    public Optional<OtpSession> findByDestination(String tenantId, Channel channel, String destination) {
        String value = redis.opsForValue().get(destinationKey(tenantId, channel, destination));
        if (value == null) {
            return Optional.empty();
        }
        String[] parts = value.split("\\|", 3);
        if (parts.length != 3) {
            return Optional.empty();
        }
        UUID sessionId = UUID.fromString(parts[0]);
        return parseSession(
                tenantId,
                sessionId,
                channel.name() + "|" + destination + "|" + parts[1] + "|" + parts[2]);
    }

    @Override
    public void delete(String tenantId, Channel channel, String destination, UUID sessionId) {
        redis.delete(destinationKey(tenantId, channel, destination));
        redis.delete(sessionKey(tenantId, sessionId));
    }

    @Override
    public int incrementFailedAttempts(String tenantId, Channel channel, String destination, Duration ttl) {
        String key = attemptsKey(tenantId, channel, destination);
        Long current = redis.opsForValue().increment(key);
        if (current != null && current == 1L) {
            redis.expire(key, ttl);
        }
        return current != null ? current.intValue() : 0;
    }

    @Override
    public void clearFailedAttempts(String tenantId, Channel channel, String destination) {
        redis.delete(attemptsKey(tenantId, channel, destination));
    }

    @Override
    public void enforceResendCooldown(String tenantId, Channel channel, String destination, Duration cooldown) {
        String key = cooldownKey(tenantId, channel, destination);
        Long ttl = redis.getExpire(key);
        if (ttl != null && ttl > 0) {
            throw new OtpResendCooldownException(ttl);
        }
        if (Boolean.TRUE.equals(redis.hasKey(key))) {
            throw new OtpResendCooldownException(cooldown.toSeconds());
        }
    }

    @Override
    public void markResendCooldown(String tenantId, Channel channel, String destination, Duration cooldown) {
        redis.opsForValue().set(cooldownKey(tenantId, channel, destination), "1", cooldown);
    }

    private Optional<OtpSession> parseSession(String tenantId, UUID sessionId, String value) {
        if (value == null) {
            return Optional.empty();
        }
        String[] parts = value.split("\\|", 4);
        if (parts.length != 4) {
            return Optional.empty();
        }
        Instant expiresAt = Instant.ofEpochMilli(Long.parseLong(parts[3]));
        if (Instant.now().isAfter(expiresAt)) {
            return Optional.empty();
        }
        return Optional.of(
                new OtpSession(
                        tenantId,
                        sessionId,
                        Channel.valueOf(parts[0]),
                        parts[1],
                        parts[2],
                        expiresAt));
    }

    static String destinationKey(String tenantId, Channel channel, String destination) {
        return "nhub:otp:"
                + normalize(tenantId)
                + ":"
                + channel.name()
                + ":"
                + normalize(destination);
    }

    static String sessionKey(String tenantId, UUID sessionId) {
        return "nhub:otp:session:" + normalize(tenantId) + ":" + sessionId;
    }

    static String attemptsKey(String tenantId, Channel channel, String destination) {
        return "nhub:otp:attempts:"
                + normalize(tenantId)
                + ":"
                + channel.name()
                + ":"
                + normalize(destination);
    }

    static String cooldownKey(String tenantId, Channel channel, String destination) {
        return "nhub:otp:cooldown:"
                + normalize(tenantId)
                + ":"
                + channel.name()
                + ":"
                + normalize(destination);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+._-]", "_");
    }
}
