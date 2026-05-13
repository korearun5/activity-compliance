package com.activityplatform.backend.fpo.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record FpoDashboardSummaryResponse(
    UUID tenantId,
    long totalMembers,
    long activeMembers,
    long totalLandholdings,
    long activeLandholdings,
    BigDecimal totalLandAreaAcres,
    BigDecimal activeLandAreaAcres,
    BigDecimal totalCultivableAreaAcres,
    long totalPlots,
    long activePlots,
    long geoTaggedPlots,
    BigDecimal totalPlotAreaAcres,
    BigDecimal activePlotAreaAcres,
    long cropPlanCount,
    long confirmedCropPlanCount,
    BigDecimal confirmedPlannedAreaAcres,
    long demandEstimateCount,
    List<FpoAreaBreakdownResponse> cropPlanAreaByCrop,
    List<FpoAreaBreakdownResponse> cropPlanAreaBySeason,
    List<FpoAreaBreakdownResponse> cropPlanAreaByVillage,
    List<FpoInputDemandBreakdownResponse> inputDemandByInput
) {
}
