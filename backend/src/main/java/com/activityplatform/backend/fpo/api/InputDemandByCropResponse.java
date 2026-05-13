package com.activityplatform.backend.fpo.api;

import java.math.BigDecimal;
import java.util.UUID;

public record InputDemandByCropResponse(
    UUID cropId,
    String cropName,
    BigDecimal plannedAreaAcres,
    int planCount
) {
}
