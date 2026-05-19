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
@Table(name = "farmer_bank_details")
public class FarmerBankDetailsEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "farmer_profile_id", nullable = false)
  private FarmerProfileEntity farmerProfile;

  @Column(name = "account_holder_name", nullable = false)
  private String accountHolderName;

  @Column(name = "account_number", nullable = false)
  private String accountNumber;

  @Column(name = "ifsc_code", nullable = false)
  private String ifscCode;

  @Column(name = "upi_id")
  private String upiId;

  @Column(name = "bank_name", nullable = false)
  private String bankName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FarmerBankDetailsStatus status;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "verified_by_user_id")
  private UserEntity verifiedByUser;

  @Column(name = "verification_notes")
  private String verificationNotes;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected FarmerBankDetailsEntity() {
  }

  public FarmerBankDetailsEntity(
      UUID id,
      TenantEntity tenant,
      FarmerProfileEntity farmerProfile,
      String accountHolderName,
      String accountNumber,
      String ifscCode,
      String upiId,
      String bankName,
      FarmerBankDetailsStatus status,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.farmerProfile = farmerProfile;
    this.accountHolderName = accountHolderName;
    this.accountNumber = accountNumber;
    this.ifscCode = ifscCode;
    this.upiId = upiId;
    this.bankName = bankName;
    this.status = status;
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

  public String getAccountHolderName() {
    return accountHolderName;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public String getIfscCode() {
    return ifscCode;
  }

  public String getUpiId() {
    return upiId;
  }

  public String getBankName() {
    return bankName;
  }

  public FarmerBankDetailsStatus getStatus() {
    return status;
  }

  public Instant getVerifiedAt() {
    return verifiedAt;
  }

  public UserEntity getVerifiedByUser() {
    return verifiedByUser;
  }

  public String getVerificationNotes() {
    return verificationNotes;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void updateDetails(
      String accountHolderName,
      String accountNumber,
      String ifscCode,
      String upiId,
      String bankName,
      FarmerBankDetailsStatus status,
      Instant now
  ) {
    this.accountHolderName = accountHolderName;
    this.accountNumber = accountNumber;
    this.ifscCode = ifscCode;
    this.upiId = upiId;
    this.bankName = bankName;
    this.status = status;
    this.updatedAt = now;
  }

  public void updateVerification(
      FarmerBankDetailsStatus status,
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
