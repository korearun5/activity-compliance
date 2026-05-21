package com.activityplatform.backend.carbon.api;

public record CarbonActivityReportBreakdownResponse(
    String categoryCode,
    String categoryName,
    long activityCount,
    long verifiedActivities,
    long pendingActivities,
    long evidenceCount
) {
}
