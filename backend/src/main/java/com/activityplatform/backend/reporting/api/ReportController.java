package com.activityplatform.backend.reporting.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.reporting.service.ReportExportService;
import com.activityplatform.backend.reporting.service.ReportSummaryService;
import com.activityplatform.backend.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
  private final ReportExportService reportExportService;
  private final ReportSummaryService reportSummaryService;

  public ReportController(
      ReportExportService reportExportService,
      ReportSummaryService reportSummaryService
  ) {
    this.reportExportService = reportExportService;
    this.reportSummaryService = reportSummaryService;
  }

  @GetMapping("/summary")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER')")
  ApiResponse<ReportSummaryResponse> summary(Authentication authentication) {
    return ApiResponse.success(reportSummaryService.summary(CurrentUser.from(authentication)));
  }

  @PostMapping("/export")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER')")
  ApiResponse<ReportExportResponse> export(
      Authentication authentication,
      @Valid @RequestBody ReportExportRequest request
  ) {
    return ApiResponse.success(reportExportService.export(CurrentUser.from(authentication), request));
  }
}
