package com.optimizesolux.notificationhub.application;

import com.optimizesolux.notificationhub.api.NotFoundException;
import com.optimizesolux.notificationhub.api.dto.SmsTestRecipientResponse;
import com.optimizesolux.notificationhub.infrastructure.persistence.SmsTestRecipientEntity;
import com.optimizesolux.notificationhub.infrastructure.persistence.SmsTestRecipientRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SmsTestRecipientService {

    private static final String AUDIT_TENANT = "global";
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final SmsTestRecipientRepository repository;
    private final AuditService auditService;

    public SmsTestRecipientService(
            SmsTestRecipientRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SmsTestRecipientResponse> list() {
        return repository.findAllByOrderByEmailAsc().stream()
                .map(SmsTestRecipientService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> activeEmails() {
        return repository.findAllEmailsOrderByEmailAsc();
    }

    @Transactional
    public SmsTestRecipientResponse add(String rawEmail) {
        String email = normalize(rawEmail);
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email address: " + rawEmail);
        }
        if (repository.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalStateException("SMS test recipient already exists: " + email);
        }

        SmsTestRecipientEntity entity = new SmsTestRecipientEntity();
        entity.setId(UUID.randomUUID());
        entity.setEmail(email);
        entity.setCreatedAt(Instant.now());
        entity.setCreatedBy(resolveActor());
        repository.save(entity);

        auditService.record(
                AUDIT_TENANT,
                "SMS_TEST_RECIPIENT_ADDED",
                entity.getId().toString(),
                Map.of("email", email));
        return toResponse(entity);
    }

    @Transactional
    public void remove(UUID id) {
        SmsTestRecipientEntity entity =
                repository
                        .findById(id)
                        .orElseThrow(
                                () -> new NotFoundException("SMS test recipient not found: " + id));
        String email = entity.getEmail();
        repository.delete(entity);
        auditService.record(
                AUDIT_TENANT,
                "SMS_TEST_RECIPIENT_REMOVED",
                id.toString(),
                Map.of("email", email));
    }

    static String normalize(String rawEmail) {
        if (!StringUtils.hasText(rawEmail)) {
            throw new IllegalArgumentException("Email is required");
        }
        return rawEmail.trim().toLowerCase(Locale.ROOT);
    }

    private static String resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return "anonymous";
        }
        if (auth.getPrincipal() instanceof Jwt jwt) {
            String sub = jwt.getClaimAsString("preferred_username");
            if (sub == null) {
                sub = jwt.getSubject();
            }
            return sub != null ? sub : "jwt";
        }
        return auth.getName() != null ? auth.getName() : "unknown";
    }

    static SmsTestRecipientResponse toResponse(SmsTestRecipientEntity entity) {
        return new SmsTestRecipientResponse(
                entity.getId(), entity.getEmail(), entity.getCreatedAt(), entity.getCreatedBy());
    }
}
