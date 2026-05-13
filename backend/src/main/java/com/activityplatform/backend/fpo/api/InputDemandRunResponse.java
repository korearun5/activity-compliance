package com.activityplatform.backend.fpo.api;

import java.util.List;
import java.util.UUID;

public record InputDemandRunResponse(
    UUID seasonId,
    UUID cropId,
    String village,
    String planStatus,
    int plansConsidered,
    int missingRulePlanCount,
    int estimatesGenerated,
    List<InputDemandEstimateResponse> estimates
) {
}
