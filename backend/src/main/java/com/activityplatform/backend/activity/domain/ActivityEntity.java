package com.activityplatform.backend.activity.domain;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.workflow.domain.WorkflowDefinitionEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "activities")
public class ActivityEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workflow_definition_id", nullable = false)
  private WorkflowDefinitionEntity workflowDefinition;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "participant_user_id")
  private UserEntity participant;

  @Column(name = "unit_name", nullable = false)
  private String unitName;

  @Column(name = "location_name")
  private String locationName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ActivityStatus status;

  @Column(name = "started_on", nullable = false)
  private LocalDate startedOn;

  @Column(name = "expected_completion", nullable = false)
  private LocalDate expectedCompletion;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "progress_percent", nullable = false)
  private int progressPercent;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("dueOn ASC")
  private List<ActivityTaskEntity> tasks = new ArrayList<>();

  protected ActivityEntity() {
  }

  public ActivityEntity(
      UUID id,
      TenantEntity tenant,
      WorkflowDefinitionEntity workflowDefinition,
      UserEntity participant,
      String unitName,
      String locationName,
      LocalDate startedOn,
      LocalDate expectedCompletion,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.workflowDefinition = workflowDefinition;
    this.participant = participant;
    this.unitName = unitName;
    this.locationName = locationName;
    this.status = ActivityStatus.RUNNING;
    this.startedOn = startedOn;
    this.expectedCompletion = expectedCompletion;
    this.progressPercent = 0;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public TenantEntity getTenant() {
    return tenant;
  }

  public WorkflowDefinitionEntity getWorkflowDefinition() {
    return workflowDefinition;
  }

  public UserEntity getParticipant() {
    return participant;
  }

  public String getUnitName() {
    return unitName;
  }

  public String getLocationName() {
    return locationName;
  }

  public ActivityStatus getStatus() {
    return status;
  }

  public LocalDate getStartedOn() {
    return startedOn;
  }

  public LocalDate getExpectedCompletion() {
    return expectedCompletion;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public int getProgressPercent() {
    return progressPercent;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public List<ActivityTaskEntity> getTasks() {
    return tasks.stream()
        .sorted(Comparator.comparing(task -> task.getWorkflowTask().getSequenceNumber()))
        .toList();
  }

  public void addTask(ActivityTaskEntity task) {
    task.attachTo(this);
    tasks.add(task);
  }

  public void updateProgress(int progressPercent, boolean complete, Instant now) {
    this.progressPercent = progressPercent;
    if (complete) {
      this.status = ActivityStatus.COMPLETED;
      this.completedAt = now;
    }
    this.updatedAt = now;
  }
}
