package com.activityplatform.backend.fpo.api;

import java.util.Map;

public record FpoReportExportRequest(
    Map<String, Object> filters
) {
}
