package com.optimizesolux.notificationhub.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateRepository extends JpaRepository<TemplateEntity, UUID> {

    Optional<TemplateEntity> findFirstByTenantIdAndNameAndActiveTrueOrderByVersionDesc(
            String tenantId, String name);

    List<TemplateEntity> findByTenantIdOrderByNameAscVersionDesc(String tenantId);
}
