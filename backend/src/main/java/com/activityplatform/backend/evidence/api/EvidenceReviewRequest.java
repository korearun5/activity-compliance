package com.activityplatform.backend.evidence.api;

import com.activityplatform.backend.evidence.domain.EvidenceStatus;
import jakarta.validation.constraints.NotNull;

public record EvidenceReviewRequest(
    @NotNull EvidenceStatus status
) {
}
