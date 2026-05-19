package com.activityplatform.backend.farmer.api;

import com.activityplatform.backend.farmer.domain.FarmerBankDetailsEntity;
import com.activityplatform.backend.farmer.domain.FarmerBankDetailsStatus;
import java.time.Instant;
import java.util.UUID;

public record FarmerBankDetailsResponse(
    UUID id,
    UUID farmerProfileId,
    String farmerName,
    String farmerMobileNumber,
    String accountHolderName,
    String accountNumber,
    String ifscCode,
    String upiId,
    String bankName,
    FarmerBankDetailsStatus status,
    Instant verifiedAt,
    UUID verifiedByUserId,
    String verificationNotes,
    Instant createdAt,
    Instant updatedAt
) {
  public static FarmerBankDetailsResponse from(FarmerBankDetailsEntity entity) {
    return new FarmerBankDetailsResponse(
        entity.getId(),
        entity.getFarmerProfile().getId(),
        entity.getFarmerProfile().getDisplayName(),
        entity.getFarmerProfile().getMobileNumber(),
        entity.getAccountHolderName(),
        entity.getAccountNumber(),
        entity.getIfscCode(),
        entity.getUpiId(),
        entity.getBankName(),
        entity.getStatus(),
        entity.getVerifiedAt(),
        entity.getVerifiedByUser() == null ? null : entity.getVerifiedByUser().getId(),
        entity.getVerificationNotes(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
