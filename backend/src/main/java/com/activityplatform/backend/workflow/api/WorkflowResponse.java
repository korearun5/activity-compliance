package com.activityplatform.backend.workflow.api;

import com.activityplatform.backend.workflow.domain.WorkflowDefinitionEntity;
import com.activityplatform.backend.workflow.domain.WorkflowDefinitionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkflowResponse(
    UUID id,
    UUID tenantId,
    String code,
    String name,
    String domainKey,
    int durationDays,
    int version,
    WorkflowDefinitionStatus status,
    List<WorkflowTaskResponse> tasks,
    Instant createdAt,
    Instant updatedAt
) {
  public static WorkflowResponse from(WorkflowDefinitionEntity workflow) {
    return new WorkflowResponse(
        workflow.getId(),
        workflow.getTenant().getId(),
        workflow.getCode(),
        workflow.getName(),
        workflow.getDomainKey(),
        workflow.getDurationDays(),
        workflow.getVersion(),
        workflow.getStatus(),
        workflow.getTasks().stream().map(WorkflowTaskResponse::from).toList(),
        workflow.getCreatedAt(),
        workflow.getUpdatedAt()
    );
  }
}
