package com.activityplatform.backend.farmer.service;

import com.activityplatform.backend.carbon.domain.CarbonProfileEntity;
import com.activityplatform.backend.carbon.domain.CarbonRecordStatus;
import com.activityplatform.backend.carbon.repository.CarbonProfileRepository;
import com.activityplatform.backend.farmer.api.FarmerProfileCompletionResponse;
import com.activityplatform.backend.farmer.api.FarmerProfileCompletionStepResponse;
import com.activityplatform.backend.farmer.domain.FarmerBankDetailsEntity;
import com.activityplatform.backend.farmer.domain.FarmerBankDetailsStatus;
import com.activityplatform.backend.farmer.domain.FarmerProfileEntity;
import com.activityplatform.backend.farmer.repository.FarmerBankDetailsRepository;
import com.activityplatform.backend.farmer.repository.FarmerDocumentRepository;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.service.TenantModuleService;
import com.activityplatform.backend.security.CurrentUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FarmerProfileCompletionService {
  private static final String COMPLETE = "COMPLETE";
  private static final String INCOMPLETE = "INCOMPLETE";

  private final FarmerBankDetailsRepository bankDetailsRepository;
  private final CarbonProfileRepository carbonProfileRepository;
  private final FarmerDocumentRepository documentRepository;
  private final FarmerService farmerService;
  private final TenantModuleService tenantModuleService;

  public FarmerProfileCompletionService(
      FarmerBankDetailsRepository bankDetailsRepository,
      CarbonProfileRepository carbonProfileRepository,
      FarmerDocumentRepository documentRepository,
      FarmerService farmerService,
      TenantModuleService tenantModuleService
  ) {
    this.bankDetailsRepository = bankDetailsRepository;
    this.carbonProfileRepository = carbonProfileRepository;
    this.documentRepository = documentRepository;
    this.farmerService = farmerService;
    this.tenantModuleService = tenantModuleService;
  }

  @Transactional(readOnly = true)
  public FarmerProfileCompletionResponse getCurrentFarmerCompletion(CurrentUser currentUser) {
    tenantModuleService.requireEnabled(currentUser.tenantId(), ModuleCode.SUSTAINABILITY);

    FarmerProfileEntity farmerProfile = farmerService.requireByUserId(
        currentUser.tenantId(),
        currentUser.userId()
    );
    Optional<CarbonProfileEntity> carbonProfile = carbonProfileRepository
        .findFirstByTenant_IdAndFarmerProfile_IdOrderByUpdatedAtDesc(
            currentUser.tenantId(),
            farmerProfile.getId()
        )
        .or(() -> carbonProfileRepository.findByTenantIdAndUserId(
            currentUser.tenantId(),
            currentUser.userId()
        ));
    Optional<FarmerBankDetailsEntity> bankDetails = bankDetailsRepository
        .findByTenantIdAndFarmerProfileId(currentUser.tenantId(), farmerProfile.getId());
    boolean hasDocuments = documentRepository.existsByTenantIdAndFarmerProfileId(
        currentUser.tenantId(),
        farmerProfile.getId()
    );

    List<FarmerProfileCompletionStepResponse> steps = new ArrayList<>();
    steps.add(requiredStep(
        "FARMER_PROFILE",
        "Farmer profile",
        isFarmerProfileComplete(farmerProfile),
        "Canonical farmer identity and location fields are captured."
    ));
    steps.add(requiredStep(
        "CARBON_ENROLLMENT",
        "Carbon enrollment",
        carbonProfile.map(this::isCarbonProfileComplete).orElse(false),
        "Carbon enrollment is linked to this farmer profile."
    ));
    steps.add(requiredStep(
        "BANK_DETAILS",
        "Bank details",
        bankDetails.map(this::isBankDetailsComplete).orElse(false),
        bankDetails.map(this::bankDetailsDescription)
            .orElse("Bank account details are pending.")
    ));
    steps.add(requiredStep(
        "DOCUMENTS",
        "Documents",
        hasDocuments,
        hasDocuments
            ? "KYC or Carbon documents are uploaded."
            : "Upload at least one KYC or Carbon document."
    ));

    int totalRequiredSteps = (int) steps.stream()
        .filter(FarmerProfileCompletionStepResponse::required)
        .count();
    int completedRequiredSteps = (int) steps.stream()
        .filter(FarmerProfileCompletionStepResponse::required)
        .filter(FarmerProfileCompletionStepResponse::complete)
        .count();
    int completionPercentage = totalRequiredSteps == 0
        ? 0
        : Math.round((completedRequiredSteps * 100.0f) / totalRequiredSteps);

    return new FarmerProfileCompletionResponse(
        farmerProfile.getId(),
        farmerProfile.getUser().getId(),
        carbonProfile.map(CarbonProfileEntity::getId).orElse(null),
        completionPercentage,
        completedRequiredSteps,
        totalRequiredSteps,
        List.copyOf(steps),
        Instant.now()
    );
  }

  private FarmerProfileCompletionStepResponse requiredStep(
      String code,
      String label,
      boolean complete,
      String description
  ) {
    return new FarmerProfileCompletionStepResponse(
        code,
        label,
        complete ? COMPLETE : INCOMPLETE,
        complete,
        true,
        false,
        description
    );
  }

  private boolean isFarmerProfileComplete(FarmerProfileEntity profile) {
    return hasText(profile.getDisplayName())
        && hasText(profile.getMobileNumber())
        && hasText(profile.getVillage())
        && hasText(profile.getTaluka())
        && hasText(profile.getDistrictName())
        && hasText(profile.getStateName())
        && hasText(profile.getGender())
        && hasText(profile.getFarmerCategory());
  }

  private boolean isCarbonProfileComplete(CarbonProfileEntity profile) {
    return profile.getStatus() == CarbonRecordStatus.ACTIVE
        && hasText(profile.getCarbonIdentityId())
        && profile.getFarmerProfileId() != null;
  }

  private boolean isBankDetailsComplete(FarmerBankDetailsEntity bankDetails) {
    return bankDetails.getStatus() != FarmerBankDetailsStatus.REJECTED
        && hasText(bankDetails.getAccountHolderName())
        && hasText(bankDetails.getAccountNumber())
        && hasText(bankDetails.getIfscCode())
        && hasText(bankDetails.getBankName());
  }

  private String bankDetailsDescription(FarmerBankDetailsEntity bankDetails) {
    return switch (bankDetails.getStatus()) {
      case PENDING_VERIFICATION -> "Bank account details are saved and pending verification.";
      case VERIFIED -> "Bank account details are verified.";
      case REJECTED -> "Bank account details need correction.";
    };
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
