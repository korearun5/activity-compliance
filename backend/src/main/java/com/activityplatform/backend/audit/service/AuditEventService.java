package com.activityplatform.backend.audit.service;

import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.domain.AuditEventEntity;
import com.activityplatform.backend.audit.repository.AuditEventRepository;
import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditEventService {
  private final AuditEventRepository auditEventRepository;

  public AuditEventService(AuditEventRepository auditEventRepository) {
    this.auditEventRepository = auditEventRepository;
  }

  @Transactional
  public void record(
      TenantEntity tenant,
      UserEntity actor,
      String aggregateType,
      UUID aggregateId,
      AuditAction action,
      Map<String, Object> metadata
  ) {
    auditEventRepository.save(new AuditEventEntity(
        UUID.randomUUID(),
        tenant,
        actor,
        aggregateType,
        aggregateId,
        action,
        metadata,
        MDC.get("requestId"),
        Instant.now()
    ));
  }
}
