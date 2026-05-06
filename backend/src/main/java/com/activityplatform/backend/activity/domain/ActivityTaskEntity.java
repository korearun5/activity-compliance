package com.activityplatform.backend.activity.domain;

import com.activityplatform.backend.workflow.domain.TaskStatus;
import com.activityplatform.backend.workflow.domain.WorkflowTaskEntity;
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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "activity_tasks")
public class ActivityTaskEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "activity_id", nullable = false)
  private ActivityEntity activity;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workflow_task_id", nullable = false)
  private WorkflowTaskEntity workflowTask;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TaskStatus status;

  @Column(name = "due_on")
  private LocalDate dueOn;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected ActivityTaskEntity() {
  }

  public ActivityTaskEntity(
      UUID id,
      WorkflowTaskEntity workflowTask,
      TaskStatus status,
      LocalDate dueOn,
      Instant now
  ) {
    this.id = id;
    this.workflowTask = workflowTask;
    this.status = status;
    this.dueOn = dueOn;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public ActivityEntity getActivity() {
    return activity;
  }

  public WorkflowTaskEntity getWorkflowTask() {
    return workflowTask;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public LocalDate getDueOn() {
    return dueOn;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void updateStatus(TaskStatus status, Instant now) {
    this.status = status;
    this.completedAt = status == TaskStatus.DONE || status == TaskStatus.SKIPPED ? now : null;
    this.updatedAt = now;
  }

  void attachTo(ActivityEntity activity) {
    this.activity = activity;
  }
}
