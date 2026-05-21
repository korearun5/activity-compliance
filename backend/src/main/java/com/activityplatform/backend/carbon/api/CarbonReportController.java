package com.activityplatform.backend.carbon.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.carbon.service.CarbonReportExportService;
import com.activityplatform.backend.carbon.service.CarbonReportSummaryService;
import com.activityplatform.backend.reporting.api.ReportExportResponse;
import com.activityplatform.backend.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/carbon/reports")
public class CarbonReportController {
  private final CarbonReportExportService carbonReportExportService;
  private final CarbonReportSummaryService carbonReportSummaryService;

  public CarbonReportController(
      CarbonReportExportService carbonReportExportService,
      CarbonReportSummaryService carbonReportSummaryService
  ) {
    this.carbonReportExportService = carbonReportExportService;
    this.carbonReportSummaryService = carbonReportSummaryService;
  }

  @GetMapping("/summary")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<CarbonReportSummaryResponse> summary(Authentication authentication) {
    return ApiResponse.success(
        carbonReportSummaryService.summary(CurrentUser.from(authentication)));
  }

  @PostMapping("/export")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<ReportExportResponse> export(
      Authentication authentication,
      @RequestBody(required = false) CarbonReportExportRequest request
  ) {
    return ApiResponse.success(carbonReportExportService.export(
        CurrentUser.from(authentication),
        request == null ? null : request.filters()
    ));
  }
}
