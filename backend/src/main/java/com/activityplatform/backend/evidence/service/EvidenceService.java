package com.activityplatform.backend.evidence.service;

import com.activityplatform.backend.activity.domain.ActivityEntity;
import com.activityplatform.backend.activity.domain.ActivityTaskEntity;
import com.activityplatform.backend.activity.service.ActivityService;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.evidence.api.EvidenceResponse;
import com.activityplatform.backend.evidence.domain.EvidenceEntity;
import com.activityplatform.backend.evidence.domain.EvidenceStatus;
import com.activityplatform.backend.evidence.repository.EvidenceRepository;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import com.activityplatform.backend.storage.FileStorageRequest;
import com.activityplatform.backend.storage.FileStorageService;
import com.activityplatform.backend.storage.StoredFile;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EvidenceService {
  private final ActivityService activityService;
  private final AuditEventService auditEventService;
  private final EvidenceRepository evidenceRepository;
  private final FileStorageService fileStorageService;
  private final UserRepository userRepository;

  public EvidenceService(
      ActivityService activityService,
      AuditEventService auditEventService,
      EvidenceRepository evidenceRepository,
      FileStorageService fileStorageService,
      UserRepository userRepository
  ) {
    this.activityService = activityService;
    this.auditEventService = auditEventService;
    this.evidenceRepository = evidenceRepository;
    this.fileStorageService = fileStorageService;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<EvidenceResponse> list(CurrentUser currentUser, UUID activityId) {
    List<EvidenceEntity> evidence = activityId == null
        ? evidenceRepository.findByTenantIdOrderBySubmittedAtDesc(currentUser.tenantId())
        : evidenceRepository.findByTenantIdAndActivityTaskActivityIdOrderBySubmittedAtDesc(
            currentUser.tenantId(),
            activityId
        );

    return evidence.stream()
        .filter(item -> canAccess(currentUser, item))
        .map(EvidenceResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public EvidenceResponse get(CurrentUser currentUser, UUID evidenceId) {
    EvidenceEntity evidence = requireEvidence(currentUser, evidenceId);
    return EvidenceResponse.from(evidence);
  }

  @Transactional
  public EvidenceResponse upload(
      CurrentUser currentUser,
      UUID activityId,
      UUID activityTaskId,
      MultipartFile file,
      String note
  ) {
    if (file == null || file.isEmpty()) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_FAILED,
          "Evidence file is required.",
          HttpStatus.BAD_REQUEST
      );
    }

    ActivityEntity activity = activityService.requireAccessibleActivityEntity(currentUser, activityId);
    ActivityTaskEntity task = activityService.requireActivityTask(activityId, activityTaskId);
    StoredFile storedFile;

    try {
      storedFile = fileStorageService.store(new FileStorageRequest(
          currentUser.tenantId(),
          "evidence",
          activityTaskId,
          file.getOriginalFilename(),
          file.getContentType(),
          file.getSize(),
          file.getInputStream()
      ));
    } catch (IOException exception) {
      throw new ApplicationException(
          ErrorCode.FILE_STORAGE_FAILED,
          "Unable to read uploaded evidence file.",
          HttpStatus.INTERNAL_SERVER_ERROR
      );
    }

    EvidenceEntity evidence = new EvidenceEntity(
        UUID.randomUUID(),
        activity.getTenant(),
        task,
        activity.getParticipant(),
        storedFile.storageKey(),
        storedFile.originalFilename(),
        storedFile.contentType(),
        storedFile.sizeBytes(),
        normalizeOptional(note),
        Instant.now()
    );

    EvidenceEntity savedEvidence = evidenceRepository.save(evidence);
    activityService.markTaskDoneAndRecalculate(activity, task);
    auditEventService.record(
        activity.getTenant(),
        activity.getParticipant(),
        "EVIDENCE",
        savedEvidence.getId(),
        AuditAction.EVIDENCE_SUBMITTED,
        Map.of(
            "activityId", activityId.toString(),
            "activityTaskId", activityTaskId.toString(),
            "storageKey", savedEvidence.getStorageKey()));
    return EvidenceResponse.from(savedEvidence);
  }

  @Transactional
  public EvidenceResponse review(
      CurrentUser currentUser,
      UUID evidenceId,
      EvidenceStatus status
  ) {
    if (status != EvidenceStatus.APPROVED && status != EvidenceStatus.REJECTED) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_FAILED,
          "Evidence review status must be APPROVED or REJECTED.",
          HttpStatus.BAD_REQUEST
      );
    }

    EvidenceEntity evidence = requireEvidence(currentUser, evidenceId);
    UserEntity reviewer = userRepository.findById(currentUser.userId())
        .orElseThrow(() -> notFound("Reviewer user not found."));
    evidence.review(status, reviewer, Instant.now());
    EvidenceEntity savedEvidence = evidenceRepository.save(evidence);
    auditEventService.record(
        savedEvidence.getTenant(),
        reviewer,
        "EVIDENCE",
        savedEvidence.getId(),
        AuditAction.EVIDENCE_REVIEWED,
        Map.of("status", status.name()));
    return EvidenceResponse.from(savedEvidence);
  }

  private EvidenceEntity requireEvidence(CurrentUser currentUser, UUID evidenceId) {
    EvidenceEntity evidence = evidenceRepository.findByIdAndTenantId(evidenceId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Evidence not found."));

    if (!canAccess(currentUser, evidence)) {
      throw new ApplicationException(
          ErrorCode.ACCESS_DENIED,
          "You do not have access to this evidence.",
          HttpStatus.FORBIDDEN
      );
    }

    return evidence;
  }

  private boolean canAccess(CurrentUser currentUser, EvidenceEntity evidence) {
    return currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER)
        || (evidence.getParticipant() != null
            && evidence.getParticipant().getId().equals(currentUser.userId()));
  }

  private String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private ApplicationException notFound(String message) {
    return new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, message, HttpStatus.NOT_FOUND);
  }
}
