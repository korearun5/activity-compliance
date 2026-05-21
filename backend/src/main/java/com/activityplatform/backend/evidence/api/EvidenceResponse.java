package com.activityplatform.backend.evidence.api;

import com.activityplatform.backend.evidence.domain.EvidenceEntity;
import com.activityplatform.backend.evidence.domain.EvidenceStatus;
import java.time.Instant;
import java.util.UUID;

public record EvidenceResponse(
    UUID id,
    UUID tenantId,
    UUID activityId,
    UUID activityTaskId,
    String workflowName,
    String workflowDomainKey,
    String unitName,
    String locationName,
    String taskCode,
    String taskTitle,
    UUID participantUserId,
    String participantName,
    String storageKey,
    String originalFilename,
    String contentType,
    long sizeBytes,
    String note,
    EvidenceStatus status,
    Instant submittedAt,
    UUID reviewedByUserId,
    Instant reviewedAt
) {
  public static EvidenceResponse from(EvidenceEntity evidence) {
    return new EvidenceResponse(
        evidence.getId(),
        evidence.getTenant().getId(),
        evidence.getActivityTask().getActivity().getId(),
        evidence.getActivityTask().getId(),
        evidence.getActivityTask().getActivity().getWorkflowDefinition().getName(),
        evidence.getActivityTask().getActivity().getWorkflowDefinition().getDomainKey(),
        evidence.getActivityTask().getActivity().getUnitName(),
        evidence.getActivityTask().getActivity().getLocationName(),
        evidence.getActivityTask().getWorkflowTask().getCode(),
        evidence.getActivityTask().getWorkflowTask().getTitle(),
        evidence.getParticipant() == null ? null : evidence.getParticipant().getId(),
        evidence.getParticipant() == null ? null : evidence.getParticipant().getDisplayName(),
        evidence.getStorageKey(),
        evidence.getOriginalFilename(),
        evidence.getContentType(),
        evidence.getSizeBytes(),
        evidence.getNote(),
        evidence.getStatus(),
        evidence.getSubmittedAt(),
        evidence.getReviewedBy() == null ? null : evidence.getReviewedBy().getId(),
        evidence.getReviewedAt()
    );
  }
}
