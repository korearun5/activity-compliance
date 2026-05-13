package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.AdvisoryStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateFpoAdvisoryStatusRequest(
    @NotNull AdvisoryStatus status
) {
}
