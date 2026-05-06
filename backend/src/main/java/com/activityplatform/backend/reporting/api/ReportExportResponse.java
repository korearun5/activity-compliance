package com.activityplatform.backend.reporting.api;

import com.activityplatform.backend.reporting.domain.ReportExportEntity;
import com.activityplatform.backend.reporting.domain.ReportFormat;
import com.activityplatform.backend.reporting.domain.ReportStatus;
import java.time.Instant;
import java.util.UUID;

public record ReportExportResponse(
    UUID id,
    UUID tenantId,
    String reportType,
    ReportFormat format,
    ReportStatus status,
    String storageKey,
    Instant requestedAt,
    Instant completedAt
) {
  public static ReportExportResponse from(ReportExportEntity export) {
    return new ReportExportResponse(
        export.getId(),
        export.getTenant().getId(),
        export.getReportType(),
        export.getFormat(),
        export.getStatus(),
        export.getStorageKey(),
        export.getRequestedAt(),
        export.getCompletedAt()
    );
  }
}
