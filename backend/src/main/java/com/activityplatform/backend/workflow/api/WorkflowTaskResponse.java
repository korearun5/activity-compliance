package com.activityplatform.backend.workflow.api;

import com.activityplatform.backend.workflow.domain.WorkflowTaskEntity;
import java.util.UUID;

public record WorkflowTaskResponse(
    UUID id,
    String code,
    String title,
    int sequenceNumber,
    int offsetDays,
    boolean evidenceRequired
) {
  static WorkflowTaskResponse from(WorkflowTaskEntity task) {
    return new WorkflowTaskResponse(
        task.getId(),
        task.getCode(),
        task.getTitle(),
        task.getSequenceNumber(),
        task.getOffsetDays(),
        task.isEvidenceRequired()
    );
  }
}
