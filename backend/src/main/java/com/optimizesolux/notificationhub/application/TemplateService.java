package com.optimizesolux.notificationhub.application;

import com.optimizesolux.notificationhub.api.NotFoundException;
import com.optimizesolux.notificationhub.api.dto.CreateTemplateRequest;
import com.optimizesolux.notificationhub.api.dto.TemplateResponse;
import com.optimizesolux.notificationhub.config.TenantContext;
import com.optimizesolux.notificationhub.infrastructure.persistence.TemplateEntity;
import com.optimizesolux.notificationhub.infrastructure.persistence.TemplateRepository;
import com.optimizesolux.notificationhub.infrastructure.template.PebbleTemplateRenderer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final PebbleTemplateRenderer renderer;

    public TemplateService(TemplateRepository templateRepository, PebbleTemplateRenderer renderer) {
        this.templateRepository = templateRepository;
        this.renderer = renderer;
    }

    @Transactional
    public TemplateResponse create(CreateTemplateRequest request) {
        String tenantId = TenantContext.require();
        int nextVersion =
                templateRepository
                        .findFirstByTenantIdAndNameAndActiveTrueOrderByVersionDesc(
                                tenantId, request.name())
                        .map(t -> t.getVersion() + 1)
                        .orElse(1);

        // deactivate previous versions for this name
        templateRepository.findByTenantIdOrderByNameAscVersionDesc(tenantId).stream()
                .filter(t -> t.getName().equals(request.name()) && t.isActive())
                .forEach(
                        t -> {
                            t.setActive(false);
                            templateRepository.save(t);
                        });

        TemplateEntity entity = new TemplateEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setName(request.name());
        entity.setVersion(nextVersion);
        entity.setChannel(request.channel());
        entity.setEngine("PEBBLE");
        entity.setSubjectTemplate(request.subjectTemplate());
        entity.setBodyTemplate(request.bodyTemplate());
        entity.setActive(true);
        entity.setCreatedAt(Instant.now());
        templateRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> list() {
        String tenantId = TenantContext.require();
        return templateRepository.findByTenantIdOrderByNameAscVersionDesc(tenantId).stream()
                .map(TemplateService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, String> preview(String name, Map<String, Object> data) {
        String tenantId = TenantContext.require();
        TemplateEntity template =
                templateRepository
                        .findFirstByTenantIdAndNameAndActiveTrueOrderByVersionDesc(tenantId, name)
                        .orElseThrow(() -> new NotFoundException("Template not found: " + name));
        String subject =
                template.getSubjectTemplate() != null
                        ? renderer.render(template.getSubjectTemplate(), data)
                        : null;
        String body = renderer.render(template.getBodyTemplate(), data);
        return Map.of(
                "subject", subject != null ? subject : "",
                "body", body);
    }

    @Transactional(readOnly = true)
    public TemplateEntity requireActive(String tenantId, String name) {
        return templateRepository
                .findFirstByTenantIdAndNameAndActiveTrueOrderByVersionDesc(tenantId, name)
                .orElseThrow(() -> new NotFoundException("Template not found: " + name));
    }

    static TemplateResponse toResponse(TemplateEntity t) {
        return new TemplateResponse(
                t.getId(),
                t.getName(),
                t.getVersion(),
                t.getChannel(),
                t.getEngine(),
                t.getSubjectTemplate(),
                t.getBodyTemplate(),
                t.isActive(),
                t.getCreatedAt());
    }
}
