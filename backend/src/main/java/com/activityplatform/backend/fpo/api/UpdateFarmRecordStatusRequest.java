package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateFarmRecordStatusRequest(
    @NotNull
    FarmRecordStatus status
) {
}
