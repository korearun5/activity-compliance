package com.activityplatform.backend.workflow.api;

import com.activityplatform.backend.workflow.domain.WorkflowDefinitionStatus;
import jakarta.validation.constraints.NotNull;

public record WorkflowStatusRequest(
    @NotNull WorkflowDefinitionStatus status
) {
}
