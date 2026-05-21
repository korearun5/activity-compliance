package com.activityplatform.backend.carbon.service;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.service.TenantModuleService;
import com.activityplatform.backend.reporting.api.ReportExportResponse;
import com.activityplatform.backend.reporting.domain.ReportExportEntity;
import com.activityplatform.backend.reporting.domain.ReportFormat;
import com.activityplatform.backend.reporting.repository.ReportExportRepository;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.storage.FileStorageRequest;
import com.activityplatform.backend.storage.FileStorageService;
import com.activityplatform.backend.storage.StoredFile;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CarbonReportExportService {
  private static final String CARBON_REPORT_TYPE = "CARBON_OPERATIONS";
  private static final String XLSX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final AuditEventService auditEventService;
  private final CarbonReportWorkbookService workbookService;
  private final FileStorageService fileStorageService;
  private final ReportExportRepository reportExportRepository;
  private final TenantModuleService tenantModuleService;
  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;

  public CarbonReportExportService(
      AuditEventService auditEventService,
      CarbonReportWorkbookService workbookService,
      FileStorageService fileStorageService,
      ReportExportRepository reportExportRepository,
      TenantModuleService tenantModuleService,
      TenantRepository tenantRepository,
      UserRepository userRepository
  ) {
    this.auditEventService = auditEventService;
    this.workbookService = workbookService;
    this.fileStorageService = fileStorageService;
    this.reportExportRepository = reportExportRepository;
    this.tenantModuleService = tenantModuleService;
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public ReportExportResponse export(CurrentUser currentUser, Map<String, Object> filters) {
    requireCarbonExportAccess(currentUser);
    TenantEntity tenant = requireTenant(currentUser.tenantId());
    UserEntity actor = actor(currentUser);
    Instant now = Instant.now();
    ReportExportEntity export = reportExportRepository.save(new ReportExportEntity(
        UUID.randomUUID(),
        tenant,
        actor,
        CARBON_REPORT_TYPE,
        ReportFormat.XLSX,
        filters,
        now
    ));
    auditExport(export, actor, AuditAction.REPORT_EXPORT_REQUESTED);

    byte[] workbook = workbookService.buildWorkbook(currentUser, filters);
    StoredFile storedFile = fileStorageService.store(new FileStorageRequest(
        currentUser.tenantId(),
        "carbon-reports",
        export.getId(),
        filename(export),
        XLSX_CONTENT_TYPE,
        workbook.length,
        new ByteArrayInputStream(workbook)
    ));
    export.complete(storedFile.storageKey(), Instant.now());
    ReportExportEntity savedExport = reportExportRepository.save(export);
    auditExport(savedExport, actor, AuditAction.REPORT_EXPORT_COMPLETED);
    return ReportExportResponse.from(savedExport);
  }

  private void requireCarbonExportAccess(CurrentUser currentUser) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.SUSTAINABILITY);
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.REPORT_EXPORT);
    CarbonAccessPolicy.requireCarbonStaff(
        currentUser,
        "Only Carbon staff can export Carbon reports."
    );
  }

  private TenantEntity requireTenant(UUID tenantId) {
    return tenantRepository.findById(tenantId)
        .orElseThrow(() -> new ApplicationException(
            ErrorCode.RESOURCE_NOT_FOUND,
            "Tenant not found.",
            HttpStatus.NOT_FOUND
        ));
  }

  private UserEntity actor(CurrentUser currentUser) {
    return userRepository.findById(currentUser.userId()).orElse(null);
  }

  private void auditExport(
      ReportExportEntity export,
      UserEntity actor,
      AuditAction action
  ) {
    auditEventService.record(
        export.getTenant(),
        actor,
        "CARBON_REPORT_EXPORT",
        export.getId(),
        action,
        Map.of(
            "reportType", export.getReportType(),
            "format", export.getFormat().name(),
            "status", export.getStatus().name()
        )
    );
  }

  private String filename(ReportExportEntity export) {
    return export.getReportType().toLowerCase().replaceAll("[^a-z0-9-]", "-")
        + "-" + export.getId() + ".xlsx";
  }
}
