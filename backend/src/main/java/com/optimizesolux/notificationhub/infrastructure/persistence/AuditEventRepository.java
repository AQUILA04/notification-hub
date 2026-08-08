package com.optimizesolux.notificationhub.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {}
