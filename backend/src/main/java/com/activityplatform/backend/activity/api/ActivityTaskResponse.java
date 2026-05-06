package com.activityplatform.backend.activity.api;

import com.activityplatform.backend.activity.domain.ActivityTaskEntity;
import com.activityplatform.backend.workflow.domain.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ActivityTaskResponse(
    UUID id,
    UUID workflowTaskId,
    String code,
    String title,
    int sequenceNumber,
    boolean evidenceRequired,
    TaskStatus status,
    LocalDate dueOn,
    Instant completedAt
) {
  static ActivityTaskResponse from(ActivityTaskEntity task) {
    return new ActivityTaskResponse(
        task.getId(),
        task.getWorkflowTask().getId(),
        task.getWorkflowTask().getCode(),
        task.getWorkflowTask().getTitle(),
        task.getWorkflowTask().getSequenceNumber(),
        task.getWorkflowTask().isEvidenceRequired(),
        task.getStatus(),
        task.getDueOn(),
        task.getCompletedAt()
    );
  }
}
