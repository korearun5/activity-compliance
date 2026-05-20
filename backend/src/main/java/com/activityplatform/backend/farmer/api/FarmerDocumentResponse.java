package com.activityplatform.backend.farmer.api;

import com.activityplatform.backend.farmer.domain.FarmerDocumentEntity;
import com.activityplatform.backend.farmer.domain.FarmerDocumentStatus;
import com.activityplatform.backend.farmer.domain.FarmerDocumentType;
import java.time.Instant;
import java.util.UUID;

public record FarmerDocumentResponse(
    UUID id,
    UUID farmerProfileId,
    String farmerName,
    String farmerMobileNumber,
    FarmerDocumentType documentType,
    String fileUrl,
    String fileName,
    String mimeType,
    FarmerDocumentStatus status,
    String verificationNotes,
    Instant uploadedAt,
    Instant verifiedAt,
    UUID verifiedByUserId,
    Instant createdAt,
    Instant updatedAt
) {
  public static FarmerDocumentResponse from(FarmerDocumentEntity entity) {
    return new FarmerDocumentResponse(
        entity.getId(),
        entity.getFarmerProfile().getId(),
        entity.getFarmerProfile().getDisplayName(),
        entity.getFarmerProfile().getMobileNumber(),
        entity.getDocumentType(),
        entity.getFileUrl(),
        entity.getFileName(),
        entity.getMimeType(),
        entity.getStatus(),
        entity.getVerificationNotes(),
        entity.getUploadedAt(),
        entity.getVerifiedAt(),
        entity.getVerifiedByUser() == null ? null : entity.getVerifiedByUser().getId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
