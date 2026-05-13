package com.activityplatform.backend.reporting.service;

import com.activityplatform.backend.activity.domain.ActivityEntity;
import com.activityplatform.backend.activity.domain.ActivityTaskEntity;
import com.activityplatform.backend.activity.repository.ActivityRepository;
import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.evidence.domain.EvidenceEntity;
import com.activityplatform.backend.evidence.repository.EvidenceRepository;
import com.activityplatform.backend.fpo.service.FpoReportWorkbookService;
import com.activityplatform.backend.reporting.api.ReportBreakdownResponse;
import com.activityplatform.backend.reporting.api.ReportExportRequest;
import com.activityplatform.backend.reporting.api.ReportExportResponse;
import com.activityplatform.backend.reporting.api.ReportSummaryResponse;
import com.activityplatform.backend.reporting.domain.ReportExportEntity;
import com.activityplatform.backend.reporting.domain.ReportFormat;
import com.activityplatform.backend.reporting.repository.ReportExportRepository;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import com.activityplatform.backend.storage.FileStorageRequest;
import com.activityplatform.backend.storage.FileStorageService;
import com.activityplatform.backend.storage.StoredFile;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportExportService {
  private static final String DEFAULT_REPORT_TYPE = "GOVERNMENT_EVIDENCE";
  private static final String FPO_REPORT_TYPE = "FPO_OPERATIONS";
  private static final String PDF_CONTENT_TYPE = "application/pdf";
  private static final String XLSX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final ActivityRepository activityRepository;
  private final AuditEventService auditEventService;
  private final EvidenceRepository evidenceRepository;
  private final FileStorageService fileStorageService;
  private final FpoReportWorkbookService fpoReportWorkbookService;
  private final ReportExportRepository reportExportRepository;
  private final ReportSummaryService reportSummaryService;
  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;

  public ReportExportService(
      ActivityRepository activityRepository,
      AuditEventService auditEventService,
      EvidenceRepository evidenceRepository,
      FileStorageService fileStorageService,
      FpoReportWorkbookService fpoReportWorkbookService,
      ReportExportRepository reportExportRepository,
      ReportSummaryService reportSummaryService,
      TenantRepository tenantRepository,
      UserRepository userRepository
  ) {
    this.activityRepository = activityRepository;
    this.auditEventService = auditEventService;
    this.evidenceRepository = evidenceRepository;
    this.fileStorageService = fileStorageService;
    this.fpoReportWorkbookService = fpoReportWorkbookService;
    this.reportExportRepository = reportExportRepository;
    this.reportSummaryService = reportSummaryService;
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public ReportExportResponse export(CurrentUser currentUser, ReportExportRequest request) {
    requireManager(currentUser);

    TenantEntity tenant = tenantRepository.findById(currentUser.tenantId())
        .orElseThrow(() -> notFound("Tenant not found."));
    UserEntity actor = actor(currentUser);
    Instant now = Instant.now();
    ReportExportEntity export = reportExportRepository.save(new ReportExportEntity(
        UUID.randomUUID(),
        tenant,
        actor,
        reportType(request.reportType()),
        request.format(),
        request.filters(),
        now
    ));

    auditExport(export, actor, AuditAction.REPORT_EXPORT_REQUESTED);

    ExportDocument document = buildDocument(currentUser, export, request);
    StoredFile storedFile = fileStorageService.store(new FileStorageRequest(
        currentUser.tenantId(),
        "reports",
        export.getId(),
        document.filename(),
        document.contentType(),
        document.bytes().length,
        new ByteArrayInputStream(document.bytes())
    ));
    export.complete(storedFile.storageKey(), Instant.now());
    ReportExportEntity savedExport = reportExportRepository.save(export);
    auditExport(savedExport, actor, AuditAction.REPORT_EXPORT_COMPLETED);
    return ReportExportResponse.from(savedExport);
  }

  private ExportDocument buildDocument(
      CurrentUser currentUser,
      ReportExportEntity export,
      ReportExportRequest request
  ) {
    if (request.format() == ReportFormat.XLSX && isFpoReportType(export.getReportType())) {
      return new ExportDocument(
          fpoReportWorkbookService.buildWorkbook(currentUser.tenantId()),
          filename(export),
          XLSX_CONTENT_TYPE
      );
    }

    ExportDataset dataset = loadDataset(currentUser);
    return switch (request.format()) {
      case PDF -> new ExportDocument(
          buildPdf(export, dataset),
          filename(export),
          PDF_CONTENT_TYPE
      );
      case XLSX -> new ExportDocument(
          buildXlsx(currentUser, export, dataset),
          filename(export),
          XLSX_CONTENT_TYPE
      );
    };
  }

  private ExportDataset loadDataset(CurrentUser currentUser) {
    List<ActivityEntity> activities = activityRepository
        .findByTenantIdOrderByCreatedAtDesc(currentUser.tenantId())
        .stream()
        .sorted(activityComparator())
        .toList();
    List<EvidenceEntity> evidenceRecords = evidenceRepository
        .findByTenantIdOrderBySubmittedAtDesc(currentUser.tenantId())
        .stream().toList();
    Map<UUID, List<EvidenceEntity>> evidenceByTaskId = evidenceRecords.stream()
        .collect(Collectors.groupingBy(
            item -> item.getActivityTask().getId(),
            LinkedHashMap::new,
            Collectors.collectingAndThen(Collectors.toList(), this::sortEvidence)
        ));

    return new ExportDataset(activities, evidenceRecords, evidenceByTaskId);
  }

  private byte[] buildPdf(ReportExportEntity export, ExportDataset dataset) {
    List<String> lines = new java.util.ArrayList<>();

    lines.add("Report type: " + export.getReportType());
    lines.add("Generated at: " + Instant.now());
    lines.add("Tenant: " + export.getTenant().getName());
    lines.add("Export id: " + export.getId());
    lines.add("");

    for (ActivityEntity activity : dataset.activities()) {
      lines.add("Workflow: " + activity.getWorkflowDefinition().getName());
      lines.add("Activity: " + activity.getId());
      lines.add("Field Coordinator: " + label(activity.getParticipant() == null
          ? null
          : activity.getParticipant().getDisplayName()));
      lines.add("Location / unit: " + label(activity.getLocationName()) + " / " + activity.getUnitName());
      lines.add("Started: " + activity.getStartedOn() + " | Status: " + activity.getStatus()
          + " | Progress: " + activity.getProgressPercent() + "%");

      for (ActivityTaskEntity task : activity.getTasks()) {
        lines.add("  Task " + task.getWorkflowTask().getSequenceNumber()
            + ": " + task.getWorkflowTask().getTitle()
            + " | Due: " + task.getDueOn()
            + " | Status: " + task.getStatus());
        List<EvidenceEntity> taskEvidence = dataset.evidenceByTaskId()
            .getOrDefault(task.getId(), List.of());
        if (taskEvidence.isEmpty()) {
          lines.add("    Evidence: none submitted");
        } else {
          for (EvidenceEntity evidence : taskEvidence) {
            lines.add("    Evidence: " + evidence.getStatus()
                + " | Submitted: " + evidence.getSubmittedAt()
                + " | Reviewed: " + reviewedBy(evidence)
                + " | File: " + evidence.getOriginalFilename()
                + " | Storage: " + evidence.getStorageKey());
            if (evidence.getNote() != null && !evidence.getNote().isBlank()) {
              lines.add("    Note: " + evidence.getNote());
            }
          }
        }
      }
      lines.add("");
    }

    return new SimplePdfDocumentBuilder().build("Activity Evidence Report", lines);
  }

  private byte[] buildXlsx(
      CurrentUser currentUser,
      ReportExportEntity export,
      ExportDataset dataset
  ) {
    ReportSummaryResponse summary = reportSummaryService.buildSummary(
        currentUser.tenantId(),
        dataset.activities(),
        dataset.evidence()
    );

    return new SimpleXlsxWorkbookBuilder().build(List.of(
        new SimpleXlsxWorkbookBuilder.Sheet("Summary", summaryRows(export, summary)),
        new SimpleXlsxWorkbookBuilder.Sheet("Activities", activityRows(dataset.activities())),
        new SimpleXlsxWorkbookBuilder.Sheet("Task Evidence", taskEvidenceRows(dataset)),
        new SimpleXlsxWorkbookBuilder.Sheet("Workflow Breakdown", breakdownRows(summary.byWorkflow())),
        new SimpleXlsxWorkbookBuilder.Sheet("Location Breakdown", breakdownRows(summary.byLocation()))
    ));
  }

  private List<List<String>> summaryRows(
      ReportExportEntity export,
      ReportSummaryResponse summary
  ) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row("Report type", export.getReportType()));
    rows.add(row("Format", export.getFormat()));
    rows.add(row("Generated at", Instant.now()));
    rows.add(row("Tenant", export.getTenant().getName()));
    rows.add(row("Export id", export.getId()));
    rows.add(List.of());
    rows.add(row("Metric", "Value"));
    rows.add(row("Field Coordinators", summary.fieldCoordinatorCount()));
    rows.add(row("Total activities", summary.totalActivities()));
    rows.add(row("Running activities", summary.runningActivities()));
    rows.add(row("Completed activities", summary.completedActivities()));
    rows.add(row("Cancelled activities", summary.cancelledActivities()));
    rows.add(row("Total tasks", summary.totalTasks()));
    rows.add(row("Completed tasks", summary.completedTasks()));
    rows.add(row("Task completion percent", summary.taskCompletionPercent()));
    rows.add(row("Evidence records", summary.evidenceRecords()));
    rows.add(row("Submitted evidence", summary.submittedEvidence()));
    rows.add(row("Pending review evidence", summary.pendingReviewEvidence()));
    rows.add(row("Approved evidence", summary.approvedEvidence()));
    rows.add(row("Rejected evidence", summary.rejectedEvidence()));
    rows.add(row("Approved evidence percent", summary.approvedEvidencePercent()));
    return rows;
  }

  private List<List<String>> activityRows(List<ActivityEntity> activities) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row(
        "Workflow",
        "Activity ID",
        "Field Coordinator",
        "Location",
        "Unit",
        "Started on",
        "Expected completion",
        "Completed at",
        "Activity status",
        "Progress percent"
    ));

    for (ActivityEntity activity : activities) {
      rows.add(row(
          activity.getWorkflowDefinition().getName(),
          activity.getId(),
          fieldCoordinatorName(activity),
          activity.getLocationName(),
          activity.getUnitName(),
          activity.getStartedOn(),
          activity.getExpectedCompletion(),
          activity.getCompletedAt(),
          activity.getStatus(),
          activity.getProgressPercent()
      ));
    }

    return rows;
  }

  private List<List<String>> taskEvidenceRows(ExportDataset dataset) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row(
        "Workflow",
        "Activity ID",
        "Field Coordinator",
        "Location",
        "Unit",
        "Task sequence",
        "Task title",
        "Task due on",
        "Task status",
        "Evidence status",
        "Submitted at",
        "Reviewer state",
        "Reviewed at",
        "Original file",
        "Storage key",
        "Note"
    ));

    for (ActivityEntity activity : dataset.activities()) {
      for (ActivityTaskEntity task : activity.getTasks()) {
        List<EvidenceEntity> taskEvidence = dataset.evidenceByTaskId()
            .getOrDefault(task.getId(), List.of());
        if (taskEvidence.isEmpty()) {
          rows.add(taskEvidenceRow(activity, task, null));
        } else {
          for (EvidenceEntity evidence : taskEvidence) {
            rows.add(taskEvidenceRow(activity, task, evidence));
          }
        }
      }
    }

    return rows;
  }

  private List<String> taskEvidenceRow(
      ActivityEntity activity,
      ActivityTaskEntity task,
      EvidenceEntity evidence
  ) {
    return row(
        activity.getWorkflowDefinition().getName(),
        activity.getId(),
        fieldCoordinatorName(activity),
        activity.getLocationName(),
        activity.getUnitName(),
        task.getWorkflowTask().getSequenceNumber(),
        task.getWorkflowTask().getTitle(),
        task.getDueOn(),
        task.getStatus(),
        evidence == null ? "NONE" : evidence.getStatus(),
        evidence == null ? null : evidence.getSubmittedAt(),
        evidence == null ? "Not submitted" : reviewerState(evidence),
        evidence == null ? null : evidence.getReviewedAt(),
        evidence == null ? null : evidence.getOriginalFilename(),
        evidence == null ? null : evidence.getStorageKey(),
        evidence == null ? null : evidence.getNote()
    );
  }

  private List<List<String>> breakdownRows(List<ReportBreakdownResponse> items) {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row(
        "Label",
        "Activities",
        "Completed activities",
        "Evidence records",
        "Approved evidence",
        "Task completion percent"
    ));

    for (ReportBreakdownResponse item : items) {
      rows.add(row(
          item.label(),
          item.activities(),
          item.completedActivities(),
          item.evidenceRecords(),
          item.approvedEvidence(),
          item.taskCompletionPercent()
      ));
    }

    return rows;
  }

  private Comparator<ActivityEntity> activityComparator() {
    return Comparator
        .comparing((ActivityEntity activity) -> activity.getWorkflowDefinition().getName())
        .thenComparing(activity -> activity.getParticipant() == null
            ? ""
            : activity.getParticipant().getDisplayName())
        .thenComparing(ActivityEntity::getStartedOn)
        .thenComparing(ActivityEntity::getUnitName);
  }

  private List<EvidenceEntity> sortEvidence(List<EvidenceEntity> evidence) {
    return evidence.stream()
        .sorted(Comparator.comparing(EvidenceEntity::getSubmittedAt))
        .toList();
  }

  private String reviewedBy(EvidenceEntity evidence) {
    if (evidence.getReviewedBy() == null) {
      return "not reviewed";
    }

    return evidence.getReviewedBy().getDisplayName() + " at " + evidence.getReviewedAt();
  }

  private String reviewerState(EvidenceEntity evidence) {
    if (evidence.getReviewedBy() == null) {
      return "Not reviewed";
    }

    return "Reviewed by " + evidence.getReviewedBy().getDisplayName();
  }

  private String fieldCoordinatorName(ActivityEntity activity) {
    return activity.getParticipant() == null ? null : activity.getParticipant().getDisplayName();
  }

  private String filename(ReportExportEntity export) {
    return export.getReportType().toLowerCase().replaceAll("[^a-z0-9-]", "-")
        + "-" + export.getId() + "." + extension(export.getFormat());
  }

  private String extension(ReportFormat format) {
    return switch (format) {
      case PDF -> "pdf";
      case XLSX -> "xlsx";
    };
  }

  private String reportType(String value) {
    return value == null || value.isBlank() ? DEFAULT_REPORT_TYPE : value.trim();
  }

  private boolean isFpoReportType(String value) {
    return FPO_REPORT_TYPE.equalsIgnoreCase(value);
  }

  private String label(String value) {
    return value == null || value.isBlank() ? "Unassigned" : value.trim();
  }

  private List<String> row(Object... values) {
    List<String> row = new ArrayList<>();
    for (Object value : values) {
      row.add(value == null ? "" : value.toString());
    }

    return row;
  }

  private void auditExport(
      ReportExportEntity export,
      UserEntity actor,
      AuditAction action
  ) {
    auditEventService.record(
        export.getTenant(),
        actor,
        "REPORT_EXPORT",
        export.getId(),
        action,
        Map.of(
            "reportType", export.getReportType(),
            "format", export.getFormat().name(),
            "status", export.getStatus().name()
        )
    );
  }

  private void requireManager(CurrentUser currentUser) {
    if (!currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER)) {
      throw new ApplicationException(
          ErrorCode.ACCESS_DENIED,
          "Only admins and FPO managers can export reports.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  private UserEntity actor(CurrentUser currentUser) {
    return userRepository.findById(currentUser.userId()).orElse(null);
  }

  private ApplicationException notFound(String message) {
    return new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, message, HttpStatus.NOT_FOUND);
  }

  private record ExportDataset(
      List<ActivityEntity> activities,
      List<EvidenceEntity> evidence,
      Map<UUID, List<EvidenceEntity>> evidenceByTaskId
  ) {
  }

  private record ExportDocument(
      byte[] bytes,
      String filename,
      String contentType
  ) {
  }
}
