package com.optimizesolux.notificationhub.application;

import com.optimizesolux.notificationhub.api.NotFoundException;
import com.optimizesolux.notificationhub.api.dto.SmsTestRecipientResponse;
import com.optimizesolux.notificationhub.infrastructure.persistence.SmsTestRecipientEntity;
import com.optimizesolux.notificationhub.infrastructure.persistence.SmsTestRecipientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsTestRecipientServiceTest {

    @Mock private SmsTestRecipientRepository repository;
    @Mock private AuditService auditService;
    @InjectMocks private SmsTestRecipientService service;

    @Test
    void addNormalizesEmailAndPersists() {
        when(repository.findByEmailIgnoreCase("devs@example.com")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SmsTestRecipientResponse response = service.add("  Devs@Example.COM ");

        assertEquals("devs@example.com", response.email());
        ArgumentCaptor<SmsTestRecipientEntity> captor =
                ArgumentCaptor.forClass(SmsTestRecipientEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("devs@example.com", captor.getValue().getEmail());
        verify(auditService)
                .record(
                        eq("global"),
                        eq("SMS_TEST_RECIPIENT_ADDED"),
                        eq(response.id().toString()),
                        anyMap());
    }

    @Test
    void addRejectsDuplicate() {
        SmsTestRecipientEntity existing = new SmsTestRecipientEntity();
        existing.setId(UUID.randomUUID());
        existing.setEmail("sms@optimizesolux.com");
        when(repository.findByEmailIgnoreCase("sms@optimizesolux.com"))
                .thenReturn(Optional.of(existing));

        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> service.add("SMS@OptimizeSolux.com"));
        assertEquals("SMS test recipient already exists: sms@optimizesolux.com", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void addRejectsInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () -> service.add("not-an-email"));
        verify(repository, never()).save(any());
    }

    @Test
    void removeDeletesAndAudits() {
        UUID id = UUID.randomUUID();
        SmsTestRecipientEntity entity = new SmsTestRecipientEntity();
        entity.setId(id);
        entity.setEmail("ahonsueric01@gmail.com");
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        service.remove(id);

        verify(repository).delete(entity);
        verify(auditService)
                .record(eq("global"), eq("SMS_TEST_RECIPIENT_REMOVED"), eq(id.toString()), anyMap());
    }

    @Test
    void removeMissingThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.remove(id));
    }

    @Test
    void activeEmailsDelegatesToRepository() {
        when(repository.findAllEmailsOrderByEmailAsc())
                .thenReturn(List.of("a@x.com", "b@y.com"));
        assertEquals(List.of("a@x.com", "b@y.com"), service.activeEmails());
    }

    @Test
    void normalizeTrimsAndLowercases() {
        assertEquals("foo@bar.com", SmsTestRecipientService.normalize("  Foo@Bar.COM "));
    }
}
