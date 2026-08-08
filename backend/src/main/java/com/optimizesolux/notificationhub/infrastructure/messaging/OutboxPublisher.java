package com.optimizesolux.notificationhub.infrastructure.messaging;

import com.optimizesolux.notificationhub.config.NotificationHubProperties;
import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.Priority;
import com.optimizesolux.notificationhub.infrastructure.persistence.OutboxMessageEntity;
import com.optimizesolux.notificationhub.infrastructure.persistence.OutboxMessageRepository;
import jakarta.jms.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxMessageRepository outboxMessageRepository;
    private final JmsTemplate jmsTemplate;
    private final NotificationHubProperties properties;

    public OutboxPublisher(
            OutboxMessageRepository outboxMessageRepository,
            JmsTemplate jmsTemplate,
            NotificationHubProperties properties) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.jmsTemplate = jmsTemplate;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${notification-hub.outbox.poll-interval-ms:500}")
    @Transactional
    public void publishBatch() {
        Instant now = Instant.now();
        List<OutboxMessageEntity> batch =
                outboxMessageRepository.lockReady(now, properties.outbox().batchSize());
        for (OutboxMessageEntity msg : batch) {
            String destination = queueFor(msg.getChannel());
            DispatchMessage payload =
                    new DispatchMessage(
                            msg.getNotificationId().toString(),
                            msg.getTenantId(),
                            msg.getChannel().name());
            int jmsPriority = toJmsPriority(msg.getPriority());
            long scheduleAt =
                    msg.getAvailableAt() != null
                            ? msg.getAvailableAt().toEpochMilli()
                            : now.toEpochMilli();
            jmsTemplate.convertAndSend(
                    destination,
                    payload,
                    message -> {
                        message.setJMSPriority(jmsPriority);
                        // Artemis scheduled delivery (absolute epoch millis)
                        if (scheduleAt > now.toEpochMilli()) {
                            message.setLongProperty("_AMQ_SCHED_DELIVERY", scheduleAt);
                        }
                        message.setStringProperty("tenantId", msg.getTenantId());
                        message.setStringProperty("priority", msg.getPriority().name());
                        return message;
                    });
            msg.setPublishedAt(now);
            outboxMessageRepository.save(msg);
            log.debug(
                    "Outbox {} → {} priority={} jmsPriority={}",
                    msg.getId(),
                    destination,
                    msg.getPriority(),
                    jmsPriority);
        }
    }

    public static int toJmsPriority(Priority priority) {
        if (priority == null) {
            return Message.DEFAULT_PRIORITY;
        }
        return switch (priority) {
            case HIGH -> 9;
            case NORMAL -> 4;
            case LOW -> 1;
        };
    }

    private String queueFor(Channel channel) {
        return switch (channel) {
            case EMAIL -> properties.queues().email();
            case SMS -> properties.queues().sms();
            case WHATSAPP -> properties.queues().whatsapp();
        };
    }
}
