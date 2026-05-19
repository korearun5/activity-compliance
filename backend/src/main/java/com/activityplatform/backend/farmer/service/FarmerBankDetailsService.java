package com.activityplatform.backend.farmer.service;

import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.farmer.api.FarmerBankDetailsRequest;
import com.activityplatform.backend.farmer.api.FarmerBankDetailsResponse;
import com.activityplatform.backend.farmer.api.FarmerBankDetailsVerificationRequest;
import com.activityplatform.backend.farmer.domain.FarmerBankDetailsEntity;
import com.activityplatform.backend.farmer.domain.FarmerBankDetailsStatus;
import com.activityplatform.backend.farmer.domain.FarmerProfileEntity;
import com.activityplatform.backend.farmer.repository.FarmerBankDetailsRepository;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.service.TenantModuleService;
import com.activityplatform.backend.security.CurrentUser;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FarmerBankDetailsService {
  private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");

  private final AuditEventService auditEventService;
  private final FarmerBankDetailsRepository bankDetailsRepository;
  private final FarmerService farmerService;
  private final TenantModuleService tenantModuleService;
  private final UserRepository userRepository;

  public FarmerBankDetailsService(
      AuditEventService auditEventService,
      FarmerBankDetailsRepository bankDetailsRepository,
      FarmerService farmerService,
      TenantModuleService tenantModuleService,
      UserRepository userRepository
  ) {
    this.auditEventService = auditEventService;
    this.bankDetailsRepository = bankDetailsRepository;
    this.farmerService = farmerService;
    this.tenantModuleService = tenantModuleService;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public FarmerBankDetailsResponse getCurrentFarmerBankDetails(CurrentUser currentUser) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.SUSTAINABILITY);
    FarmerProfileEntity farmerProfile = farmerService.requireByUserId(
        currentUser.tenantId(),
        currentUser.userId()
    );

    return bankDetailsRepository
        .findByTenantIdAndFarmerProfileId(currentUser.tenantId(), farmerProfile.getId())
        .map(FarmerBankDetailsResponse::from)
        .orElse(null);
  }

  @Transactional(readOnly = true)
  public List<FarmerBankDetailsResponse> listPendingForAdmin(CurrentUser currentUser) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.SUSTAINABILITY);
    return bankDetailsRepository
        .findByTenantIdAndStatusOrderByUpdatedAtAsc(
            currentUser.tenantId(),
            FarmerBankDetailsStatus.PENDING_VERIFICATION
        )
        .stream()
        .map(FarmerBankDetailsResponse::from)
        .toList();
  }

  @Transactional
  public FarmerBankDetailsResponse createOrUpdateCurrentFarmerBankDetails(
      CurrentUser currentUser,
      FarmerBankDetailsRequest request
  ) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.SUSTAINABILITY);
    FarmerProfileEntity farmerProfile = farmerService.requireByUserId(
        currentUser.tenantId(),
        currentUser.userId()
    );
    NormalizedFarmerBankDetails normalized = normalize(request);
    Instant now = Instant.now();

    FarmerBankDetailsEntity saved = bankDetailsRepository
        .findByTenantIdAndFarmerProfileId(currentUser.tenantId(), farmerProfile.getId())
        .map(existing -> {
          existing.updateDetails(
              normalized.accountHolderName(),
              normalized.accountNumber(),
              normalized.ifscCode(),
              normalized.upiId(),
              normalized.bankName(),
              FarmerBankDetailsStatus.PENDING_VERIFICATION,
              now
          );
          FarmerBankDetailsEntity updated = bankDetailsRepository.saveAndFlush(existing);
          audit(currentUser, updated, AuditAction.FARMER_BANK_DETAILS_UPDATED);
          return updated;
        })
        .orElseGet(() -> {
          FarmerBankDetailsEntity created = new FarmerBankDetailsEntity(
              UUID.randomUUID(),
              farmerProfile.getTenant(),
              farmerProfile,
              normalized.accountHolderName(),
              normalized.accountNumber(),
              normalized.ifscCode(),
              normalized.upiId(),
              normalized.bankName(),
              FarmerBankDetailsStatus.PENDING_VERIFICATION,
              now
          );
          FarmerBankDetailsEntity added = bankDetailsRepository.saveAndFlush(created);
          audit(currentUser, added, AuditAction.FARMER_BANK_DETAILS_ADDED);
          return added;
        });

    return FarmerBankDetailsResponse.from(saved);
  }

  @Transactional
  public FarmerBankDetailsResponse updateCurrentFarmerBankDetails(
      CurrentUser currentUser,
      UUID bankDetailsId,
      FarmerBankDetailsRequest request
  ) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.SUSTAINABILITY);
    FarmerProfileEntity farmerProfile = farmerService.requireByUserId(
        currentUser.tenantId(),
        currentUser.userId()
    );
    FarmerBankDetailsEntity bankDetails = bankDetailsRepository
        .findByIdAndTenantIdAndFarmerProfileId(
            bankDetailsId,
            currentUser.tenantId(),
            farmerProfile.getId()
        )
        .orElseThrow(() -> notFound("Bank details not found."));
    NormalizedFarmerBankDetails normalized = normalize(request);
    bankDetails.updateDetails(
        normalized.accountHolderName(),
        normalized.accountNumber(),
        normalized.ifscCode(),
        normalized.upiId(),
        normalized.bankName(),
        FarmerBankDetailsStatus.PENDING_VERIFICATION,
        Instant.now()
    );
    FarmerBankDetailsEntity saved = bankDetailsRepository.saveAndFlush(bankDetails);
    audit(currentUser, saved, AuditAction.FARMER_BANK_DETAILS_UPDATED);
    return FarmerBankDetailsResponse.from(saved);
  }

  @Transactional
  public FarmerBankDetailsResponse verifyForAdmin(
      CurrentUser currentUser,
      UUID bankDetailsId,
      FarmerBankDetailsVerificationRequest request
  ) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.SUSTAINABILITY);
    FarmerBankDetailsStatus nextStatus = normalizeVerificationStatus(request);
    UserEntity actor = actor(currentUser);
    FarmerBankDetailsEntity bankDetails = bankDetailsRepository
        .findByIdAndTenantId(bankDetailsId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Bank details not found."));

    bankDetails.updateVerification(
        nextStatus,
        actor,
        normalizeOptional(request.notes()),
        Instant.now()
    );
    FarmerBankDetailsEntity saved = bankDetailsRepository.saveAndFlush(bankDetails);
    audit(currentUser, saved, AuditAction.FARMER_BANK_DETAILS_VERIFIED);
    return FarmerBankDetailsResponse.from(saved);
  }

  private NormalizedFarmerBankDetails normalize(FarmerBankDetailsRequest request) {
    if (request == null) {
      throw validation("Bank details are required.");
    }

    String accountHolderName = normalizeRequired(
        request.accountHolderName(),
        "Account holder name"
    );
    String accountNumber = normalizeRequired(request.accountNumber(), "Account number");
    String ifscCode = normalizeRequired(request.ifscCode(), "IFSC code").toUpperCase();
    if (!IFSC_PATTERN.matcher(ifscCode).matches()) {
      throw validation("IFSC code format is invalid.");
    }

    return new NormalizedFarmerBankDetails(
        accountHolderName,
        accountNumber,
        ifscCode,
        normalizeOptional(request.upiId()),
        normalizeRequired(request.bankName(), "Bank name")
    );
  }

  private FarmerBankDetailsStatus normalizeVerificationStatus(
      FarmerBankDetailsVerificationRequest request
  ) {
    if (request == null || request.status() == null) {
      throw validation("Verification status is required.");
    }

    if (request.status() == FarmerBankDetailsStatus.PENDING_VERIFICATION) {
      throw validation("Verification status must be VERIFIED or REJECTED.");
    }

    return request.status();
  }

  private String normalizeRequired(String value, String label) {
    if (value == null || value.isBlank()) {
      throw validation(label + " is required.");
    }
    return value.trim();
  }

  private String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private void audit(
      CurrentUser currentUser,
      FarmerBankDetailsEntity bankDetails,
      AuditAction action
  ) {
    auditEventService.record(
        bankDetails.getTenant(),
        actor(currentUser),
        "FARMER_BANK_DETAILS",
        bankDetails.getId(),
        action,
        Map.of(
            "farmerProfileId",
            bankDetails.getFarmerProfile().getId().toString(),
            "status",
            bankDetails.getStatus().name()
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

  private record NormalizedFarmerBankDetails(
      String accountHolderName,
      String accountNumber,
      String ifscCode,
      String upiId,
      String bankName
  ) {
  }
}
