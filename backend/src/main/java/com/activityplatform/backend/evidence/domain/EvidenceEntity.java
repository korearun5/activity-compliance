package com.activityplatform.backend.evidence.domain;

import com.activityplatform.backend.activity.domain.ActivityTaskEntity;
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
@Table(name = "evidence")
public class EvidenceEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "activity_task_id", nullable = false)
  private ActivityTaskEntity activityTask;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "participant_user_id")
  private UserEntity participant;

  @Column(name = "storage_key", nullable = false)
  private String storageKey;

  @Column(name = "original_filename", nullable = false)
  private String originalFilename;

  @Column(name = "content_type", nullable = false)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  private String note;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EvidenceStatus status;

  @Column(name = "submitted_at", nullable = false)
  private Instant submittedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewed_by_user_id")
  private UserEntity reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  protected EvidenceEntity() {
  }

  public EvidenceEntity(
      UUID id,
      TenantEntity tenant,
      ActivityTaskEntity activityTask,
      UserEntity participant,
      String storageKey,
      String originalFilename,
      String contentType,
      long sizeBytes,
      String note,
      Instant submittedAt
  ) {
    this.id = id;
    this.tenant = tenant;
    this.activityTask = activityTask;
    this.participant = participant;
    this.storageKey = storageKey;
    this.originalFilename = originalFilename;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
    this.note = note;
    this.status = EvidenceStatus.SUBMITTED;
    this.submittedAt = submittedAt;
  }

  public UUID getId() {
    return id;
  }

  public TenantEntity getTenant() {
    return tenant;
  }

  public ActivityTaskEntity getActivityTask() {
    return activityTask;
  }

  public UserEntity getParticipant() {
    return participant;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public String getOriginalFilename() {
    return originalFilename;
  }

  public String getContentType() {
    return contentType;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public String getNote() {
    return note;
  }

  public EvidenceStatus getStatus() {
    return status;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public UserEntity getReviewedBy() {
    return reviewedBy;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public void review(EvidenceStatus status, UserEntity reviewer, Instant now) {
    this.status = status;
    this.reviewedBy = reviewer;
    this.reviewedAt = now;
  }
}
