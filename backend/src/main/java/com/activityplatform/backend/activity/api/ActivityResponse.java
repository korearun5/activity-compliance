package com.activityplatform.backend.activity.api;

import com.activityplatform.backend.activity.domain.ActivityEntity;
import com.activityplatform.backend.activity.domain.ActivityStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ActivityResponse(
    UUID id,
    UUID tenantId,
    UUID workflowDefinitionId,
    String workflowName,
    String workflowDomainKey,
    UUID participantUserId,
    String participantName,
    String unitName,
    String locationName,
    ActivityStatus status,
    LocalDate startedOn,
    LocalDate expectedCompletion,
    Instant completedAt,
    int progressPercent,
    List<ActivityTaskResponse> tasks,
    Instant createdAt,
    Instant updatedAt
) {
  public static ActivityResponse from(ActivityEntity activity) {
    return new ActivityResponse(
        activity.getId(),
        activity.getTenant().getId(),
        activity.getWorkflowDefinition().getId(),
        activity.getWorkflowDefinition().getName(),
        activity.getWorkflowDefinition().getDomainKey(),
        activity.getParticipant() == null ? null : activity.getParticipant().getId(),
        activity.getParticipant() == null ? null : activity.getParticipant().getDisplayName(),
        activity.getUnitName(),
        activity.getLocationName(),
        activity.getStatus(),
        activity.getStartedOn(),
        activity.getExpectedCompletion(),
        activity.getCompletedAt(),
        activity.getProgressPercent(),
        activity.getTasks().stream().map(ActivityTaskResponse::from).toList(),
        activity.getCreatedAt(),
        activity.getUpdatedAt()
    );
  }
}
