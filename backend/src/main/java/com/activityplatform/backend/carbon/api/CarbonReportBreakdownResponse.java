package com.activityplatform.backend.carbon.api;

import java.math.BigDecimal;

public record CarbonReportBreakdownResponse(
    String label,
    long profileCount,
    long plotCount,
    BigDecimal areaAcres,
    long soilProfileCount,
    long activityCount,
    long pendingVerificationCount
) {
}
