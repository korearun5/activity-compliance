package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.CropPlanStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCropPlanStatusRequest(
    @NotNull
    CropPlanStatus status
) {
}
