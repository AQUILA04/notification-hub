package com.optimizesolux.notificationhub.application;

import com.optimizesolux.notificationhub.infrastructure.persistence.AuditEventEntity;
import com.optimizesolux.notificationhub.infrastructure.persistence.AuditEventRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public void record(String tenantId, String action, String resourceId, Map<String, Object> payload) {
        AuditEventEntity event = new AuditEventEntity();
        event.setTenantId(tenantId);
        event.setAction(action);
        event.setResourceId(resourceId);
        event.setActor(resolveActor());
        event.setPayload(payload);
        auditEventRepository.save(event);
    }

    private String resolveActor() {
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
}
