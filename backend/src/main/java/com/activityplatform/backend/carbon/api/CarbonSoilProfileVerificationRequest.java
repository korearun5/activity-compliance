package com.activityplatform.backend.carbon.api;

import com.activityplatform.backend.carbon.domain.CarbonVerificationStatus;
import jakarta.validation.constraints.NotNull;

public record CarbonSoilProfileVerificationRequest(
    @NotNull CarbonVerificationStatus status,
    String notes
) {
}
