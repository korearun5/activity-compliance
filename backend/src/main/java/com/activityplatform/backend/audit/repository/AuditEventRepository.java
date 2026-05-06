package com.activityplatform.backend.audit.repository;

import com.activityplatform.backend.audit.domain.AuditEventEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {
}
