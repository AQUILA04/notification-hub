package com.optimizesolux.notificationhub.otp.infrastructure;

import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.otp.domain.OtpSession;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface OtpStore {

    void save(OtpSession session, Duration ttl);

    Optional<OtpSession> findBySessionId(String tenantId, UUID sessionId);

    Optional<OtpSession> findByDestination(String tenantId, Channel channel, String destination);

    void delete(String tenantId, Channel channel, String destination, UUID sessionId);

    /** @return current failed attempt count after increment */
    int incrementFailedAttempts(String tenantId, Channel channel, String destination, Duration ttl);

    void clearFailedAttempts(String tenantId, Channel channel, String destination);

    /** @throws com.optimizesolux.notificationhub.otp.api.OtpResendCooldownException if cooldown active */
    void enforceResendCooldown(String tenantId, Channel channel, String destination, Duration cooldown);

    void markResendCooldown(String tenantId, Channel channel, String destination, Duration cooldown);
}
