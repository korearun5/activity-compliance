package com.activityplatform.backend.carbon.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CarbonReportSummaryResponse(
    UUID tenantId,
    long totalProfiles,
    long activeProfiles,
    long linkedFarmerLogins,
    BigDecimal totalLandHoldingAcres,
    long totalPlots,
    long activePlots,
    BigDecimal totalPlotAreaAcres,
    long soilProfileCount,
    long verifiedSoilProfiles,
    long pendingSoilProfiles,
    BigDecimal averageSoilOrganicCarbonPercent,
    long activityCount,
    long verifiedActivities,
    long pendingActivities,
    long evidenceCount,
    List<CarbonReportBreakdownResponse> villageBreakdowns,
    List<CarbonActivityReportBreakdownResponse> activityBreakdowns
) {
}
