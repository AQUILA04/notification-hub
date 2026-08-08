package com.optimizesolux.notificationhub.infrastructure.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** Provider SMS de développement : log uniquement, aucun appel réseau. */
@Component
public class LoggingSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsProvider.class);

    @Override
    public String id() {
        return "logging";
    }

    @Override
    public String send(String from, List<String> to, String body) {
        String id = "sms-log-" + UUID.randomUUID();
        log.info("SMS (logging) id={} from={} to={} body={}", id, from, to, body);
        return id;
    }
}
