package com.activityplatform.backend.workflow.repository;

import com.activityplatform.backend.workflow.domain.WorkflowTaskEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowTaskRepository extends JpaRepository<WorkflowTaskEntity, UUID> {
  List<WorkflowTaskEntity> findByWorkflowDefinitionIdOrderBySequenceNumber(UUID workflowDefinitionId);
}
