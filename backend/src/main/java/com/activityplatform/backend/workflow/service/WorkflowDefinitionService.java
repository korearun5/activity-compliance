package com.activityplatform.backend.workflow.service;

import com.activityplatform.backend.activity.repository.ActivityRepository;
import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.workflow.api.WorkflowRequest;
import com.activityplatform.backend.workflow.api.WorkflowResponse;
import com.activityplatform.backend.workflow.api.WorkflowTaskRequest;
import com.activityplatform.backend.workflow.domain.WorkflowDefinitionEntity;
import com.activityplatform.backend.workflow.domain.WorkflowDefinitionStatus;
import com.activityplatform.backend.workflow.domain.WorkflowTaskEntity;
import com.activityplatform.backend.workflow.repository.WorkflowDefinitionRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowDefinitionService {
  private final ActivityRepository activityRepository;
  private final AuditEventService auditEventService;
  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;
  private final WorkflowDefinitionRepository workflowDefinitionRepository;

  public WorkflowDefinitionService(
      ActivityRepository activityRepository,
      AuditEventService auditEventService,
      TenantRepository tenantRepository,
      UserRepository userRepository,
      WorkflowDefinitionRepository workflowDefinitionRepository) {
    this.activityRepository = activityRepository;
    this.auditEventService = auditEventService;
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
    this.workflowDefinitionRepository = workflowDefinitionRepository;
  }

  @Transactional(readOnly = true)
  public Page<WorkflowResponse> list(
      CurrentUser currentUser,
      String domain,
      WorkflowDefinitionStatus status,
      Pageable pageable) {
    String domainKey = normalizeOptional(domain);
    if (domainKey != null) {
      if (status == null) {
        return workflowDefinitionRepository.findByTenantIdAndDomainKey(
            currentUser.tenantId(),
            domainKey,
            pageable).map(WorkflowResponse::from);
      }

      return workflowDefinitionRepository.findByTenantIdAndDomainKeyAndStatus(
          currentUser.tenantId(),
          domainKey,
          status,
          pageable).map(WorkflowResponse::from);
    }

    if (status == null) {
      return workflowDefinitionRepository.findByTenantId(currentUser.tenantId(), pageable)
          .map(WorkflowResponse::from);
    }

    return workflowDefinitionRepository.findByTenantIdAndStatus(
        currentUser.tenantId(),
        status,
        pageable).map(WorkflowResponse::from);
  }

  @Transactional(readOnly = true)
  public WorkflowResponse get(CurrentUser currentUser, UUID workflowId) {
    return WorkflowResponse.from(findByIdAndTenant(currentUser, workflowId));
  }

  @Transactional(readOnly = true)
  public WorkflowDefinitionEntity requireActive(CurrentUser currentUser, UUID workflowId) {
    WorkflowDefinitionEntity workflow = findByIdAndTenant(currentUser, workflowId);
    if (workflow.getStatus() != WorkflowDefinitionStatus.ACTIVE) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_FAILED,
          "Workflow must be active before an activity can be started.",
          HttpStatus.BAD_REQUEST);
    }
    return workflow;
  }

  private WorkflowDefinitionEntity findByIdAndTenant(CurrentUser currentUser, UUID workflowId) {
    return workflowDefinitionRepository.findByIdAndTenantId(workflowId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Workflow definition not found."));
  }

  @Transactional
  public WorkflowResponse create(CurrentUser currentUser, WorkflowRequest request) {
    validateTasks(request.tasks());
    String code = normalizeCode(request.code());

    if (workflowDefinitionRepository.existsByTenantIdAndCodeIgnoreCaseAndVersion(
        currentUser.tenantId(),
        code,
        request.version())) {
      throw new ApplicationException(
          ErrorCode.DUPLICATE_RESOURCE,
          "Workflow code and version already exist for this tenant.",
          HttpStatus.CONFLICT);
    }

    TenantEntity tenant = tenantRepository.findById(currentUser.tenantId())
        .orElseThrow(() -> notFound("Tenant not found."));
    Instant now = Instant.now();
    WorkflowDefinitionEntity workflow = new WorkflowDefinitionEntity(
        UUID.randomUUID(),
        tenant,
        code,
        request.name().trim(),
        normalizeOptional(request.domainKey()),
        request.durationDays(),
        request.version(),
        request.status() == null ? WorkflowDefinitionStatus.DRAFT : request.status(),
        now);
    toTaskEntities(request.tasks(), now).forEach(workflow::addTask);
    WorkflowDefinitionEntity savedWorkflow = workflowDefinitionRepository.save(workflow);
    auditWorkflowChanged(currentUser, savedWorkflow, "created");
    return WorkflowResponse.from(savedWorkflow);
  }

  @Transactional
  public WorkflowResponse update(
      CurrentUser currentUser,
      UUID workflowId,
      WorkflowRequest request) {
    validateTasks(request.tasks());
    WorkflowDefinitionEntity workflow = findByIdAndTenant(currentUser, workflowId);

    if (!workflow.getCode().equalsIgnoreCase(request.code().trim())
        || workflow.getVersion() != request.version()) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_FAILED,
          "Workflow code and version are immutable. Create a new version instead.",
          HttpStatus.BAD_REQUEST);
    }

    if (activityRepository.existsByWorkflowDefinitionId(workflowId)) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_FAILED,
          "This workflow already has activities. Create a new workflow version for task changes.",
          HttpStatus.BAD_REQUEST);
    }

    Instant now = Instant.now();
    workflow.updateDetails(
        request.name().trim(),
        normalizeOptional(request.domainKey()),
        request.durationDays(),
        request.status() == null ? workflow.getStatus() : request.status(),
        now);
    workflow.replaceTasks(toTaskEntities(request.tasks(), now), now);
    WorkflowDefinitionEntity savedWorkflow = workflowDefinitionRepository.save(workflow);
    auditWorkflowChanged(currentUser, savedWorkflow, "updated");
    return WorkflowResponse.from(savedWorkflow);
  }

  @Transactional
  public WorkflowResponse updateStatus(
      CurrentUser currentUser,
      UUID workflowId,
      WorkflowDefinitionStatus status) {
    WorkflowDefinitionEntity workflow = findByIdAndTenant(currentUser, workflowId);
    workflow.updateStatus(status, Instant.now());
    WorkflowDefinitionEntity savedWorkflow = workflowDefinitionRepository.save(workflow);
    auditWorkflowChanged(currentUser, savedWorkflow, "status_changed");
    return WorkflowResponse.from(savedWorkflow);
  }

  private void auditWorkflowChanged(
      CurrentUser currentUser,
      WorkflowDefinitionEntity workflow,
      String operation) {
    auditEventService.record(
        workflow.getTenant(),
        actor(currentUser),
        "WORKFLOW",
        workflow.getId(),
        AuditAction.WORKFLOW_CHANGED,
        Map.of(
            "operation", operation,
            "code", workflow.getCode(),
            "version", workflow.getVersion(),
            "status", workflow.getStatus().name()));
  }

  private List<WorkflowTaskEntity> toTaskEntities(
      List<WorkflowTaskRequest> tasks,
      Instant now) {
    return tasks.stream()
        .map(task -> new WorkflowTaskEntity(
            UUID.randomUUID(),
            normalizeCode(task.code()),
            task.title().trim(),
            task.sequenceNumber(),
            task.offsetDays(),
            task.evidenceRequired(),
            now))
        .toList();
  }

  private void validateTasks(List<WorkflowTaskRequest> tasks) {
    Set<String> codes = new HashSet<>();
    Set<Integer> sequences = new HashSet<>();

    for (WorkflowTaskRequest task : tasks) {
      String code = normalizeCode(task.code());

      if (!codes.add(code)) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_FAILED,
            "Task codes must be unique within a workflow.",
            HttpStatus.BAD_REQUEST);
      }

      if (!sequences.add(task.sequenceNumber())) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_FAILED,
            "Task sequence numbers must be unique within a workflow.",
            HttpStatus.BAD_REQUEST);
      }
    }
  }

  private String normalizeCode(String code) {
    return code.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
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
