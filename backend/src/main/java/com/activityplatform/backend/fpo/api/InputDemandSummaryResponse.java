package com.activityplatform.backend.fpo.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InputDemandSummaryResponse(
    UUID seasonId,
    UUID cropId,
    String village,
    int planCount,
    int memberCount,
    int estimateCount,
    BigDecimal totalPlannedAreaAcres,
    List<InputDemandByInputResponse> byInput,
    List<InputDemandByCropResponse> byCrop,
    List<InputDemandByVillageResponse> byVillage
) {
}
