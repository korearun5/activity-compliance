package com.activityplatform.backend.farmer.service;

import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.farmer.api.FarmerDocumentResponse;
import com.activityplatform.backend.farmer.api.FarmerDocumentVerificationRequest;
import com.activityplatform.backend.farmer.domain.FarmerDocumentEntity;
import com.activityplatform.backend.farmer.domain.FarmerDocumentStatus;
import com.activityplatform.backend.farmer.domain.FarmerDocumentType;
import com.activityplatform.backend.farmer.domain.FarmerProfileEntity;
import com.activityplatform.backend.farmer.repository.FarmerDocumentRepository;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.service.TenantModuleService;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.storage.FileStorageRequest;
import com.activityplatform.backend.storage.FileStorageService;
import com.activityplatform.backend.storage.StoredFile;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FarmerDocumentService {
  private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;
  private static final long MAX_PDF_BYTES = 10L * 1024L * 1024L;

  private final AuditEventService auditEventService;
  private final FarmerDocumentRepository documentRepository;
  private final FarmerService farmerService;
  private final FileStorageService fileStorageService;
  private final TenantModuleService tenantModuleService;
  private final UserRepository userRepository;

  public FarmerDocumentService(
      AuditEventService auditEventService,
      FarmerDocumentRepository documentRepository,
      FarmerService farmerService,
      FileStorageService fileStorageService,
      TenantModuleService tenantModuleService,
      UserRepository userRepository
  ) {
    this.auditEventService = auditEventService;
    this.documentRepository = documentRepository;
    this.farmerService = farmerService;
    this.fileStorageService = fileStorageService;
    this.tenantModuleService = tenantModuleService;
    this.userRepository = userRepository;
  }

  @Transactional
  public FarmerDocumentResponse uploadCurrentFarmerDocument(
      CurrentUser currentUser,
      FarmerDocumentType documentType,
      MultipartFile file
  ) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.SUSTAINABILITY);
    FarmerProfileEntity farmerProfile = farmerService.requireByUserId(
        currentUser.tenantId(),
        currentUser.userId()
    );
    validateDocumentType(documentType);
    validateFile(file);

    StoredFile storedFile;
    try {
      storedFile = fileStorageService.store(new FileStorageRequest(
          currentUser.tenantId(),
          "farmer-document",
          farmerProfile.getId(),
          file.getOriginalFilename(),
          file.getContentType(),
          file.getSize(),
          file.getInputStream()
      ));
    } catch (IOException exception) {
      throw new ApplicationException(
          ErrorCode.FILE_STORAGE_FAILED,
          "Unable to read uploaded farmer document.",
          HttpStatus.INTERNAL_SERVER_ERROR
      );
    }

    Instant now = Instant.now();
    FarmerDocumentEntity document = new FarmerDocumentEntity(
        UUID.randomUUID(),
        farmerProfile.getTenant(),
        farmerProfile,
        documentType,
        storedFile.storageKey(),
        storedFile.originalFilename(),
        storedFile.contentType(),
        FarmerDocumentStatus.PENDING_VERIFICATION,
        now
    );

    FarmerDocumentEntity saved = documentRepository.saveAndFlush(document);
    audit(currentUser, saved, AuditAction.FARMER_DOCUMENT_UPLOADED);
    return FarmerDocumentResponse.from(saved);
  }

  @Transactional(readOnly = true)
  public List<FarmerDocumentResponse> listCurrentFarmerDocuments(CurrentUser currentUser) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.SUSTAINABILITY);
    FarmerProfileEntity farmerProfile = farmerService.requireByUserId(
        currentUser.tenantId(),
        currentUser.userId()
    );

    return documentRepository
        .findByTenantIdAndFarmerProfileIdOrderByUploadedAtDesc(
            currentUser.tenantId(),
            farmerProfile.getId()
        )
        .stream()
        .map(FarmerDocumentResponse::from)
        .toList();
  }

  @Transactional
  public void deleteCurrentFarmerDocument(CurrentUser currentUser, UUID documentId) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.SUSTAINABILITY);
    FarmerProfileEntity farmerProfile = farmerService.requireByUserId(
        currentUser.tenantId(),
        currentUser.userId()
    );
    FarmerDocumentEntity document = documentRepository
        .findByIdAndTenantIdAndFarmerProfileId(
            documentId,
            currentUser.tenantId(),
            farmerProfile.getId()
        )
        .orElseThrow(() -> notFound("Farmer document not found."));

    if (document.getStatus() != FarmerDocumentStatus.PENDING_VERIFICATION) {
      throw validation("Only pending documents can be deleted.");
    }

    audit(currentUser, document, AuditAction.FARMER_DOCUMENT_DELETED);
    documentRepository.delete(document);
  }

  @Transactional(readOnly = true)
  public List<FarmerDocumentResponse> listPendingForAdmin(CurrentUser currentUser) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.SUSTAINABILITY);
    return documentRepository
        .findByTenantIdAndStatusOrderByUploadedAtAsc(
            currentUser.tenantId(),
            FarmerDocumentStatus.PENDING_VERIFICATION
        )
        .stream()
        .map(FarmerDocumentResponse::from)
        .toList();
  }

  @Transactional
  public FarmerDocumentResponse verifyForAdmin(
      CurrentUser currentUser,
      UUID documentId,
      FarmerDocumentVerificationRequest request
  ) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.SUSTAINABILITY);
    FarmerDocumentStatus nextStatus = normalizeVerificationStatus(request);
    UserEntity actor = actor(currentUser);
    FarmerDocumentEntity document = documentRepository
        .findByIdAndTenantId(documentId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Farmer document not found."));

    document.updateVerification(
        nextStatus,
        actor,
        normalizeOptional(request.notes()),
        Instant.now()
    );
    FarmerDocumentEntity saved = documentRepository.saveAndFlush(document);
    audit(currentUser, saved, AuditAction.FARMER_DOCUMENT_VERIFIED);
    return FarmerDocumentResponse.from(saved);
  }

  private void validateDocumentType(FarmerDocumentType documentType) {
    if (documentType == null) {
      throw validation("Document type is required.");
    }
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw validation("Farmer document file is required.");
    }

    String contentType = normalizeContentType(file.getContentType());
    long maxSize = switch (contentType) {
      case "application/pdf" -> MAX_PDF_BYTES;
      default -> contentType.startsWith("image/") ? MAX_IMAGE_BYTES : -1L;
    };

    if (maxSize < 0) {
      throw validation("Farmer documents must be images or PDFs.");
    }

    if (file.getSize() > maxSize) {
      throw validation(contentType.equals("application/pdf")
          ? "PDF documents must be 10 MB or smaller."
          : "Image documents must be 5 MB or smaller.");
    }
  }

  private FarmerDocumentStatus normalizeVerificationStatus(
      FarmerDocumentVerificationRequest request
  ) {
    if (request == null || request.status() == null) {
      throw validation("Verification status is required.");
    }

    if (request.status() == FarmerDocumentStatus.PENDING_VERIFICATION) {
      throw validation("Verification status must be VERIFIED or REJECTED.");
    }

    return request.status();
  }

  private String normalizeContentType(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      throw validation("Uploaded file content type is required.");
    }
    return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
  }

  private String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private void audit(
      CurrentUser currentUser,
      FarmerDocumentEntity document,
      AuditAction action
  ) {
    auditEventService.record(
        document.getTenant(),
        actor(currentUser),
        "FARMER_DOCUMENT",
        document.getId(),
        action,
        Map.of(
            "farmerProfileId",
            document.getFarmerProfile().getId().toString(),
            "documentType",
            document.getDocumentType().name(),
            "status",
            document.getStatus().name()
        )
    );
  }

  private UserEntity actor(CurrentUser currentUser) {
    return userRepository.findByIdAndTenantId(currentUser.userId(), currentUser.tenantId())
        .orElse(null);
  }

  private ApplicationException validation(String message) {
    return new ApplicationException(ErrorCode.VALIDATION_FAILED, message, HttpStatus.BAD_REQUEST);
  }

  private ApplicationException notFound(String message) {
    return new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, message, HttpStatus.NOT_FOUND);
  }
}
