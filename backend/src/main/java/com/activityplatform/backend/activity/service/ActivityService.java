package com.activityplatform.backend.activity.service;

import com.activityplatform.backend.activity.api.ActivityResponse;
import com.activityplatform.backend.activity.api.StartActivityRequest;
import com.activityplatform.backend.activity.domain.ActivityEntity;
import com.activityplatform.backend.activity.domain.ActivityStatus;
import com.activityplatform.backend.activity.domain.ActivityTaskEntity;
import com.activityplatform.backend.activity.repository.ActivityRepository;
import com.activityplatform.backend.activity.repository.ActivityTaskRepository;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import com.activityplatform.backend.workflow.domain.TaskProgress;
import com.activityplatform.backend.workflow.domain.TaskStatus;
import com.activityplatform.backend.workflow.domain.WorkflowDefinitionEntity;
import com.activityplatform.backend.workflow.domain.WorkflowDefinitionStatus;
import com.activityplatform.backend.workflow.repository.WorkflowDefinitionRepository;
import com.activityplatform.backend.workflow.service.WorkflowProgressionService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {
  private final ActivityRepository activityRepository;
  private final ActivityTaskRepository activityTaskRepository;
  private final AuditEventService auditEventService;
  private final UserRepository userRepository;
  private final WorkflowDefinitionRepository workflowDefinitionRepository;
  private final WorkflowProgressionService workflowProgressionService;

  public ActivityService(
      ActivityRepository activityRepository,
      ActivityTaskRepository activityTaskRepository,
      AuditEventService auditEventService,
      UserRepository userRepository,
      WorkflowDefinitionRepository workflowDefinitionRepository,
      WorkflowProgressionService workflowProgressionService) {
    this.activityRepository = activityRepository;
    this.activityTaskRepository = activityTaskRepository;
    this.auditEventService = auditEventService;
    this.userRepository = userRepository;
    this.workflowDefinitionRepository = workflowDefinitionRepository;
    this.workflowProgressionService = workflowProgressionService;
  }

  @Transactional(readOnly = true)
  public Page<ActivityResponse> list(CurrentUser currentUser, ActivityStatus status, Pageable pageable) {
    Page<ActivityEntity> activities;
    if (currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER)) {
      activities = status == null
          ? activityRepository.findByTenantId(currentUser.tenantId(), pageable)
          : activityRepository.findByTenantIdAndStatus(currentUser.tenantId(), status, pageable);
    } else {
      activities = status == null
          ? activityRepository.findByTenantIdAndParticipantId(currentUser.tenantId(), currentUser.userId(), pageable)
          : activityRepository.findByTenantIdAndParticipantIdAndStatus(
              currentUser.tenantId(),
              currentUser.userId(),
              status,
              pageable);
    }

    return activities.map(ActivityResponse::from);
  }

  @Transactional(readOnly = true)
  public ActivityResponse get(CurrentUser currentUser, UUID activityId) {
    return ActivityResponse.from(requireAccessibleActivity(currentUser, activityId));
  }

  @Transactional
  public ActivityResponse start(CurrentUser currentUser, StartActivityRequest request) {
    WorkflowDefinitionEntity workflow = workflowDefinitionRepository
        .findByIdAndTenantId(request.workflowDefinitionId(), currentUser.tenantId())
        .orElseThrow(() -> notFound("Workflow definition not found."));

    if (workflow.getStatus() == WorkflowDefinitionStatus.ACTIVE) {
      ActivityEntity activity = createActivity(currentUser, request, workflow);
      auditEventService.record(
          activity.getTenant(),
          activity.getParticipant(),
          "ACTIVITY",
          activity.getId(),
          AuditAction.ACTIVITY_CREATED,
          Map.of(
              "workflowDefinitionId", workflow.getId().toString(),
              "unitName", activity.getUnitName()));
      return ActivityResponse.from(activity);
    }

    throw new ApplicationException(
        ErrorCode.VALIDATION_FAILED,
        "Workflow must be active before an activity can be started.",
        HttpStatus.BAD_REQUEST);
  }

  @Transactional
  public ActivityResponse updateTaskStatus(
      CurrentUser currentUser,
      UUID activityId,
      UUID activityTaskId,
      TaskStatus status) {
    if (status != TaskStatus.DONE && status != TaskStatus.SKIPPED) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_FAILED,
          "Task status can only be changed to DONE or SKIPPED through this endpoint.",
          HttpStatus.BAD_REQUEST);
    }

    ActivityEntity activity = requireAccessibleActivity(currentUser, activityId);
    ActivityTaskEntity task = activityTaskRepository.findByIdAndActivityId(activityTaskId, activityId)
        .orElseThrow(() -> notFound("Activity task not found."));
    task.updateStatus(status, Instant.now());
    updateActivityProgress(activity);
    ActivityEntity savedActivity = activityRepository.save(activity);
    auditEventService.record(
        savedActivity.getTenant(),
        actor(currentUser),
        "ACTIVITY_TASK",
        task.getId(),
        AuditAction.STATUS_CHANGED,
        Map.of(
            "activityId", activityId.toString(),
            "taskCode", task.getWorkflowTask().getCode(),
            "status", status.name()));
    return ActivityResponse.from(savedActivity);
  }

  public ActivityEntity requireAccessibleActivityEntity(CurrentUser currentUser, UUID activityId) {
    return requireAccessibleActivity(currentUser, activityId);
  }

  public ActivityTaskEntity requireActivityTask(UUID activityId, UUID activityTaskId) {
    return activityTaskRepository.findByIdAndActivityId(activityTaskId, activityId)
        .orElseThrow(() -> notFound("Activity task not found."));
  }

  public void markTaskDoneAndRecalculate(ActivityEntity activity, ActivityTaskEntity task) {
    task.updateStatus(TaskStatus.DONE, Instant.now());
    updateActivityProgress(activity);
    activityRepository.save(activity);
  }

  private ActivityEntity createActivity(
      CurrentUser currentUser,
      StartActivityRequest request,
      WorkflowDefinitionEntity workflow) {
    Instant now = Instant.now();
    LocalDate startedOn = request.startedOn() == null ? LocalDate.now() : request.startedOn();
    UserEntity fieldCoordinator = resolveFieldCoordinator(currentUser, request.participantUserId());
    ActivityEntity activity = new ActivityEntity(
        UUID.randomUUID(),
        workflow.getTenant(),
        workflow,
        fieldCoordinator,
        request.unitName().trim(),
        normalizeOptional(request.locationName()),
        startedOn,
        startedOn.plusDays(workflow.getDurationDays()),
        now);

    List<TaskProgress> activeTasks = workflowProgressionService.activateNextPending(
        workflow.getTasks().stream()
            .map(task -> new TaskProgress(task.getCode(), task.getSequenceNumber(), TaskStatus.PENDING))
            .toList());
    Map<String, TaskStatus> statusByCode = activeTasks.stream()
        .collect(Collectors.toMap(TaskProgress::taskCode, TaskProgress::status));

    workflow.getTasks().forEach(template -> activity.addTask(new ActivityTaskEntity(
        UUID.randomUUID(),
        template,
        statusByCode.get(template.getCode()),
        startedOn.plusDays(template.getOffsetDays()),
        now)));

    return activityRepository.save(activity);
  }

  private UserEntity resolveFieldCoordinator(CurrentUser currentUser, UUID fieldCoordinatorUserId) {
    UUID requestedUserId = fieldCoordinatorUserId == null ? currentUser.userId() : fieldCoordinatorUserId;

    if (!currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER)
        && !requestedUserId.equals(currentUser.userId())) {
      throw new ApplicationException(
          ErrorCode.ACCESS_DENIED,
          "Field coordinators can only create activities for themselves.",
          HttpStatus.FORBIDDEN);
    }

    UserEntity fieldCoordinator = userRepository.findById(requestedUserId)
        .orElseThrow(() -> notFound("Field coordinator user not found."));

    if (!fieldCoordinator.getTenant().getId().equals(currentUser.tenantId())) {
      throw new ApplicationException(
          ErrorCode.ACCESS_DENIED,
          "Field coordinator user belongs to another tenant.",
          HttpStatus.FORBIDDEN);
    }

    return fieldCoordinator;
  }

  private ActivityEntity requireAccessibleActivity(CurrentUser currentUser, UUID activityId) {
    ActivityEntity activity = activityRepository.findByIdAndTenantId(activityId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Activity not found."));

    if (!currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER)
        && (activity.getParticipant() == null
            || !activity.getParticipant().getId().equals(currentUser.userId()))) {
      throw new ApplicationException(
          ErrorCode.ACCESS_DENIED,
          "You do not have access to this activity.",
          HttpStatus.FORBIDDEN);
    }

    return activity;
  }

  private void updateActivityProgress(ActivityEntity activity) {
    List<TaskProgress> activatedTasks = workflowProgressionService.activateNextPending(
        activity.getTasks().stream()
            .map(task -> new TaskProgress(
                task.getWorkflowTask().getCode(),
                task.getWorkflowTask().getSequenceNumber(),
                task.getStatus()))
            .toList());
    Map<String, TaskProgress> progressByCode = activatedTasks.stream()
        .collect(Collectors.toMap(TaskProgress::taskCode, Function.identity()));
    Instant now = Instant.now();

    activity.getTasks().forEach(task -> {
      TaskStatus nextStatus = progressByCode.get(task.getWorkflowTask().getCode()).status();
      if (task.getStatus() != nextStatus) {
        task.updateStatus(nextStatus, now);
      }
    });

    activity.updateProgress(
        workflowProgressionService.calculateProgressPercent(activatedTasks),
        workflowProgressionService.isComplete(activatedTasks),
        now);
  }

  private String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private UserEntity actor(CurrentUser currentUser) {
    return userRepository.findById(currentUser.userId()).orElse(null);
  }

  private ApplicationException notFound(String message) {
    return new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, message, HttpStatus.NOT_FOUND);
  }
}
