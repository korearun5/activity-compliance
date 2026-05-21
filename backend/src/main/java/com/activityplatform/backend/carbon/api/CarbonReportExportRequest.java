package com.activityplatform.backend.carbon.api;

import java.util.Map;

public record CarbonReportExportRequest(
    Map<String, Object> filters
) {
}
