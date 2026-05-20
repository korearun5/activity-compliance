package com.activityplatform.backend.farmer.domain;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "farmer_documents")
public class FarmerDocumentEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "farmer_profile_id", nullable = false)
  private FarmerProfileEntity farmerProfile;

  @Enumerated(EnumType.STRING)
  @Column(name = "document_type", nullable = false)
  private FarmerDocumentType documentType;

  @Column(name = "file_url", nullable = false)
  private String fileUrl;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "mime_type", nullable = false)
  private String mimeType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FarmerDocumentStatus status;

  @Column(name = "verification_notes")
  private String verificationNotes;

  @Column(name = "uploaded_at", nullable = false)
  private Instant uploadedAt;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "verified_by_user_id")
  private UserEntity verifiedByUser;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected FarmerDocumentEntity() {
  }

  public FarmerDocumentEntity(
      UUID id,
      TenantEntity tenant,
      FarmerProfileEntity farmerProfile,
      FarmerDocumentType documentType,
      String fileUrl,
      String fileName,
      String mimeType,
      FarmerDocumentStatus status,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.farmerProfile = farmerProfile;
    this.documentType = documentType;
    this.fileUrl = fileUrl;
    this.fileName = fileName;
    this.mimeType = mimeType;
    this.status = status;
    this.uploadedAt = now;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public TenantEntity getTenant() {
    return tenant;
  }

  public FarmerProfileEntity getFarmerProfile() {
    return farmerProfile;
  }

  public FarmerDocumentType getDocumentType() {
    return documentType;
  }

  public String getFileUrl() {
    return fileUrl;
  }

  public String getFileName() {
    return fileName;
  }

  public String getMimeType() {
    return mimeType;
  }

  public FarmerDocumentStatus getStatus() {
    return status;
  }

  public String getVerificationNotes() {
    return verificationNotes;
  }

  public Instant getUploadedAt() {
    return uploadedAt;
  }

  public Instant getVerifiedAt() {
    return verifiedAt;
  }

  public UserEntity getVerifiedByUser() {
    return verifiedByUser;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void updateVerification(
      FarmerDocumentStatus status,
      UserEntity verifiedByUser,
      String verificationNotes,
      Instant now
  ) {
    this.status = status;
    this.verifiedByUser = verifiedByUser;
    this.verifiedAt = now;
    this.verificationNotes = verificationNotes;
    this.updatedAt = now;
  }
}
