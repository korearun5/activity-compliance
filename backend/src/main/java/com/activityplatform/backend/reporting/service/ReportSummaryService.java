package com.activityplatform.backend.reporting.service;

import com.activityplatform.backend.activity.domain.ActivityEntity;
import com.activityplatform.backend.activity.domain.ActivityStatus;
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
import com.activityplatform.backend.evidence.domain.EvidenceStatus;
import com.activityplatform.backend.evidence.repository.EvidenceRepository;
import com.activityplatform.backend.reporting.api.ReportBreakdownResponse;
import com.activityplatform.backend.reporting.api.ReportSummaryResponse;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import com.activityplatform.backend.workflow.domain.TaskStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportSummaryService {
  private final ActivityRepository activityRepository;
  private final AuditEventService auditEventService;
  private final EvidenceRepository evidenceRepository;
  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;

  public ReportSummaryService(
      ActivityRepository activityRepository,
      AuditEventService auditEventService,
      EvidenceRepository evidenceRepository,
      TenantRepository tenantRepository,
      UserRepository userRepository
  ) {
    this.activityRepository = activityRepository;
    this.auditEventService = auditEventService;
    this.evidenceRepository = evidenceRepository;
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public ReportSummaryResponse summary(CurrentUser currentUser) {
    requireManager(currentUser);

    TenantEntity tenant = tenantRepository.findById(currentUser.tenantId())
        .orElseThrow(() -> notFound("Tenant not found."));
    List<ActivityEntity> activities = activityRepository
        .findByTenantIdOrderByCreatedAtDesc(currentUser.tenantId());
    List<EvidenceEntity> evidence = evidenceRepository
        .findByTenantIdOrderBySubmittedAtDesc(currentUser.tenantId());
    ReportSummaryResponse response = buildSummary(tenant.getId(), activities, evidence);

    auditEventService.record(
        tenant,
        actor(currentUser),
        "REPORT",
        tenant.getId(),
        AuditAction.REPORT_SUMMARY_VIEWED,
        Map.of(
            "totalActivities", response.totalActivities(),
            "evidenceRecords", response.evidenceRecords(),
            "approvedEvidence", response.approvedEvidence()
        )
    );

    return response;
  }

  ReportSummaryResponse buildSummary(
      UUID tenantId,
      List<ActivityEntity> activities,
      List<EvidenceEntity> evidence
  ) {
    long fieldCoordinatorCount = userRepository.countByTenantIdAndRoleCode(
        tenantId,
        Role.FIELD_COORDINATOR.name()
    );
    long totalTasks = activities.stream().mapToLong(activity -> activity.getTasks().size()).sum();
    long completedTasks = activities.stream()
        .flatMap(activity -> activity.getTasks().stream())
        .filter(ReportSummaryService::isTaskComplete)
        .count();
    long approvedEvidence = countEvidence(evidence, EvidenceStatus.APPROVED);
    long rejectedEvidence = countEvidence(evidence, EvidenceStatus.REJECTED);
    long submittedEvidence = countEvidence(evidence, EvidenceStatus.SUBMITTED);
    long pendingReviewEvidence = submittedEvidence + countEvidence(evidence, EvidenceStatus.PENDING_REVIEW);

    return new ReportSummaryResponse(
        tenantId,
        fieldCoordinatorCount,
        activities.size(),
        countActivities(activities, ActivityStatus.RUNNING),
        countActivities(activities, ActivityStatus.COMPLETED),
        countActivities(activities, ActivityStatus.CANCELLED),
        totalTasks,
        completedTasks,
        evidence.size(),
        submittedEvidence,
        pendingReviewEvidence,
        approvedEvidence,
        rejectedEvidence,
        percent(completedTasks, totalTasks),
        percent(approvedEvidence, evidence.size()),
        breakdownByWorkflow(activities, evidence),
        breakdownByLocation(activities, evidence)
    );
  }

  private List<ReportBreakdownResponse> breakdownByWorkflow(
      List<ActivityEntity> activities,
      List<EvidenceEntity> evidence
  ) {
    return breakdown(
        activities,
        evidence,
        activity -> activity.getWorkflowDefinition().getName()
    );
  }

  private List<ReportBreakdownResponse> breakdownByLocation(
      List<ActivityEntity> activities,
      List<EvidenceEntity> evidence
  ) {
    return breakdown(
        activities,
        evidence,
        activity -> normalizeLabel(activity.getLocationName(), "Unassigned location")
    );
  }

  private List<ReportBreakdownResponse> breakdown(
      List<ActivityEntity> activities,
      List<EvidenceEntity> evidence,
      Function<ActivityEntity, String> labelResolver
  ) {
    Map<String, List<ActivityEntity>> activitiesByLabel = activities.stream()
        .collect(Collectors.groupingBy(labelResolver));
    Map<UUID, ActivityEntity> activityById = activities.stream()
        .collect(Collectors.toMap(ActivityEntity::getId, Function.identity()));
    Map<String, List<EvidenceEntity>> evidenceByLabel = evidence.stream()
        .collect(Collectors.groupingBy(item -> {
          UUID activityId = item.getActivityTask().getActivity().getId();
          ActivityEntity activity = activityById.get(activityId);
          return activity == null ? "Unassigned" : labelResolver.apply(activity);
        }));

    return activitiesByLabel.entrySet().stream()
        .map(entry -> {
          List<ActivityEntity> groupedActivities = entry.getValue();
          List<EvidenceEntity> groupedEvidence = evidenceByLabel.getOrDefault(entry.getKey(), List.of());
          long totalTasks = groupedActivities.stream()
              .mapToLong(activity -> activity.getTasks().size())
              .sum();
          long completedTasks = groupedActivities.stream()
              .flatMap(activity -> activity.getTasks().stream())
              .filter(ReportSummaryService::isTaskComplete)
              .count();

          return new ReportBreakdownResponse(
              entry.getKey(),
              groupedActivities.size(),
              countActivities(groupedActivities, ActivityStatus.COMPLETED),
              groupedEvidence.size(),
              countEvidence(groupedEvidence, EvidenceStatus.APPROVED),
              percent(completedTasks, totalTasks)
          );
        })
        .sorted(Comparator.comparing(ReportBreakdownResponse::label))
        .toList();
  }

  private static boolean isTaskComplete(ActivityTaskEntity task) {
    return task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.SKIPPED;
  }

  private static long countActivities(List<ActivityEntity> activities, ActivityStatus status) {
    return activities.stream().filter(activity -> activity.getStatus() == status).count();
  }

  private static long countEvidence(List<EvidenceEntity> evidence, EvidenceStatus status) {
    return evidence.stream().filter(item -> item.getStatus() == status).count();
  }

  private static int percent(long count, long total) {
    if (total <= 0) {
      return 0;
    }

    return Math.toIntExact(Math.round((count * 100.0) / total));
  }

  private static String normalizeLabel(String value, String fallback) {
    return value == null || value.isBlank()
        ? fallback
        : value.trim();
  }

  private void requireManager(CurrentUser currentUser) {
    if (!currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER)) {
      throw new ApplicationException(
          ErrorCode.ACCESS_DENIED,
          "Only admins and FPO managers can view report summaries.",
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
}
