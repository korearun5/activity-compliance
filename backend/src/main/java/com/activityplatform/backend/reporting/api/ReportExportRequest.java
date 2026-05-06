package com.activityplatform.backend.reporting.api;

import com.activityplatform.backend.reporting.domain.ReportFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record ReportExportRequest(
    @NotNull ReportFormat format,
    @Size(max = 80) String reportType,
    Map<String, Object> filters
) {
}
