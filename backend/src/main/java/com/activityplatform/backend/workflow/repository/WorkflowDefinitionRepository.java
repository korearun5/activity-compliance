package com.activityplatform.backend.workflow.repository;

import com.activityplatform.backend.workflow.domain.WorkflowDefinitionEntity;
import com.activityplatform.backend.workflow.domain.WorkflowDefinitionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowDefinitionRepository
    extends JpaRepository<WorkflowDefinitionEntity, UUID> {
  List<WorkflowDefinitionEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

  List<WorkflowDefinitionEntity> findByTenantIdAndStatusOrderByCreatedAtDesc(
      UUID tenantId,
      WorkflowDefinitionStatus status);

  // Paginated queries
  Page<WorkflowDefinitionEntity> findByTenantId(UUID tenantId, Pageable pageable);

  Page<WorkflowDefinitionEntity> findByTenantIdAndStatus(
      UUID tenantId,
      WorkflowDefinitionStatus status,
      Pageable pageable);

  Page<WorkflowDefinitionEntity> findByTenantIdAndDomainKey(
      UUID tenantId,
      String domainKey,
      Pageable pageable);

  Page<WorkflowDefinitionEntity> findByTenantIdAndDomainKeyAndStatus(
      UUID tenantId,
      String domainKey,
      WorkflowDefinitionStatus status,
      Pageable pageable);

  Optional<WorkflowDefinitionEntity> findByIdAndTenantId(UUID id, UUID tenantId);

  boolean existsByTenantIdAndCodeIgnoreCaseAndVersion(UUID tenantId, String code, int version);
}
