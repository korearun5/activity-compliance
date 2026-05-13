package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.fpo.service.FpoDashboardSummaryService;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.service.TenantModuleService;
import com.activityplatform.backend.reporting.api.ReportExportRequest;
import com.activityplatform.backend.reporting.api.ReportExportResponse;
import com.activityplatform.backend.reporting.domain.ReportFormat;
import com.activityplatform.backend.reporting.service.ReportExportService;
import com.activityplatform.backend.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fpo/reports")
public class FpoReportController {
  private static final String FPO_REPORT_TYPE = "FPO_OPERATIONS";

  private final FpoDashboardSummaryService dashboardSummaryService;
  private final ReportExportService reportExportService;
  private final TenantModuleService tenantModuleService;

  public FpoReportController(
      FpoDashboardSummaryService dashboardSummaryService,
      ReportExportService reportExportService,
      TenantModuleService tenantModuleService
  ) {
    this.dashboardSummaryService = dashboardSummaryService;
    this.reportExportService = reportExportService;
    this.tenantModuleService = tenantModuleService;
  }

  @GetMapping("/summary")
  @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
  ApiResponse<FpoDashboardSummaryResponse> summary(Authentication authentication) {
    return ApiResponse.success(
        dashboardSummaryService.summary(CurrentUser.from(authentication)));
  }

  @PostMapping("/export")
  @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
  ApiResponse<ReportExportResponse> export(
      Authentication authentication,
      @RequestBody(required = false) FpoReportExportRequest request
  ) {
    CurrentUser currentUser = CurrentUser.from(authentication);
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.REPORT_EXPORT);
    return ApiResponse.success(reportExportService.export(
        currentUser,
        new ReportExportRequest(
            ReportFormat.XLSX,
            FPO_REPORT_TYPE,
            request == null ? null : request.filters()
        )
    ));
  }
}
