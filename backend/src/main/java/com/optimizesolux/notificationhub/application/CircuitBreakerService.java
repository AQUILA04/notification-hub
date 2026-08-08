package com.optimizesolux.notificationhub.application;

import com.optimizesolux.notificationhub.api.CircuitOpenException;
import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

/**
 * Circuit breaker Redis (partagé entre pods) par clé provider/canal.
 *
 * <p>CLOSED → échecs incrémentés ; seuil atteint → OPEN pendant {@code openDurationSeconds}.
 * OPEN → refus immédiat. À l'expiration → HALF_OPEN (1 tentative) ; succès → CLOSED.
 */
@Service
public class CircuitBreakerService {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerService.class);

    private final StringRedisTemplate redis;
    private final NotificationHubProperties properties;

    public CircuitBreakerService(StringRedisTemplate redis, NotificationHubProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public void beforeCall(String key) {
        NotificationHubProperties.CircuitBreaker cfg = properties.circuitBreaker();
        if (cfg == null || !cfg.enabled()) {
            return;
        }
        String stateKey = stateKey(key);
        String state = redis.opsForValue().get(stateKey);
        if ("OPEN".equals(state)) {
            String openedAt = redis.opsForValue().get(openedAtKey(key));
            if (openedAt != null) {
                Instant openInstant = Instant.parse(openedAt);
                if (openInstant
                        .plusSeconds(cfg.openDurationSeconds())
                        .isAfter(Instant.now())) {
                    throw new CircuitOpenException(key);
                }
            }
            redis.opsForValue().set(stateKey, "HALF_OPEN", Duration.ofSeconds(cfg.openDurationSeconds()));
            log.info("Circuit HALF_OPEN for {}", key);
            return;
        }
        if ("HALF_OPEN".equals(state)) {
            // une seule tentative concurrente : si déjà marqué probing, refuse
            Boolean probing =
                    redis.opsForValue()
                            .setIfAbsent(probingKey(key), "1", Duration.ofSeconds(30));
            if (Boolean.FALSE.equals(probing)) {
                throw new CircuitOpenException(key);
            }
        }
    }

    public void onSuccess(String key) {
        NotificationHubProperties.CircuitBreaker cfg = properties.circuitBreaker();
        if (cfg == null || !cfg.enabled()) {
            return;
        }
        redis.delete(failuresKey(key));
        redis.delete(openedAtKey(key));
        redis.delete(probingKey(key));
        redis.opsForValue().set(stateKey(key), "CLOSED", Duration.ofHours(24));
    }

    public void onFailure(String key) {
        NotificationHubProperties.CircuitBreaker cfg = properties.circuitBreaker();
        if (cfg == null || !cfg.enabled()) {
            return;
        }
        String state = redis.opsForValue().get(stateKey(key));
        if ("HALF_OPEN".equals(state)) {
            open(key, cfg);
            return;
        }
        Long failures = redis.opsForValue().increment(failuresKey(key));
        if (failures != null && failures == 1L) {
            redis.expire(failuresKey(key), Duration.ofMinutes(10));
        }
        if (failures != null && failures >= cfg.failureThreshold()) {
            open(key, cfg);
        }
    }

    public Map<String, String> snapshot(String... keys) {
        java.util.LinkedHashMap<String, String> out = new java.util.LinkedHashMap<>();
        for (String key : keys) {
            String state = redis.opsForValue().get(stateKey(key));
            out.put(key, state != null ? state : "CLOSED");
        }
        return out;
    }

    private void open(String key, NotificationHubProperties.CircuitBreaker cfg) {
        redis.opsForValue()
                .set(stateKey(key), "OPEN", Duration.ofSeconds(cfg.openDurationSeconds() + 60L));
        redis.opsForValue()
                .set(
                        openedAtKey(key),
                        Instant.now().toString(),
                        Duration.ofSeconds(cfg.openDurationSeconds() + 60L));
        redis.delete(probingKey(key));
        log.warn("Circuit OPEN for {} (threshold={})", key, cfg.failureThreshold());
    }

    private static String stateKey(String key) {
        return "nhub:cb:" + normalize(key) + ":state";
    }

    private static String failuresKey(String key) {
        return "nhub:cb:" + normalize(key) + ":failures";
    }

    private static String openedAtKey(String key) {
        return "nhub:cb:" + normalize(key) + ":openedAt";
    }

    private static String probingKey(String key) {
        return "nhub:cb:" + normalize(key) + ":probing";
    }

    private static String normalize(String key) {
        return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._:-]", "_");
    }
}
