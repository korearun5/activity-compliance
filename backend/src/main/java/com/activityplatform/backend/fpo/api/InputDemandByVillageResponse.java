package com.activityplatform.backend.fpo.api;

import java.math.BigDecimal;

public record InputDemandByVillageResponse(
    String village,
    BigDecimal plannedAreaAcres,
    int memberCount,
    int planCount
) {
}
