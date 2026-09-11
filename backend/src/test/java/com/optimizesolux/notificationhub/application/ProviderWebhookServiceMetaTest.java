package com.optimizesolux.notificationhub.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimizesolux.notificationhub.domain.Channel;
import com.optimizesolux.notificationhub.domain.NotificationStatus;
import com.optimizesolux.notificationhub.infrastructure.persistence.NotificationEntity;
import com.optimizesolux.notificationhub.infrastructure.persistence.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderWebhookServiceMetaTest {

    @Mock NotificationRepository notificationRepository;
    @Mock EventStoreService eventStoreService;

    @Test
    void mapsDeliveredStatusByWamid() {
        ProviderWebhookService service =
                new ProviderWebhookService(
                        notificationRepository, eventStoreService, new ObjectMapper());

        UUID id = UUID.randomUUID();
        NotificationEntity entity = new NotificationEntity();
        entity.setId(id);
        entity.setTenantId("demo");
        entity.setChannel(Channel.WHATSAPP);
        entity.setStatus(NotificationStatus.SENT);
        entity.setProviderMessageId("wamid.HBgMMjI4OTIxODEzNTE");

        when(notificationRepository.findByProviderMessageId("wamid.HBgMMjI4OTIxODEzNTE"))
                .thenReturn(Optional.of(entity));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String body =
                """
                {"object":"whatsapp_business_account","entry":[{"changes":[{"value":{"statuses":[{"id":"wamid.HBgMMjI4OTIxODEzNTE","status":"delivered","timestamp":"1710000000"}]}}]}]}
                """;

        service.handleMetaWebhook(body);

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(NotificationStatus.DELIVERED, captor.getValue().getStatus());
    }
}
