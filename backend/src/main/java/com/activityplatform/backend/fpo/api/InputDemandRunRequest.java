package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.CropPlanStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record InputDemandRunRequest(
    @NotNull
    UUID seasonId,
    UUID cropId,
    @Size(max = 160)
    String village,
    CropPlanStatus planStatus
) {
}
