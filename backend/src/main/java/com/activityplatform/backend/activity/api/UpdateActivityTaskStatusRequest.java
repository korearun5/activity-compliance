package com.activityplatform.backend.activity.api;

import com.activityplatform.backend.workflow.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateActivityTaskStatusRequest(
    @NotNull TaskStatus status
) {
}
