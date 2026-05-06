package com.activityplatform.backend.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_tasks")
public class WorkflowTaskEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workflow_definition_id", nullable = false)
  private WorkflowDefinitionEntity workflowDefinition;

  @Column(nullable = false)
  private String code;

  @Column(nullable = false)
  private String title;

  @Column(name = "sequence_number", nullable = false)
  private int sequenceNumber;

  @Column(name = "offset_days", nullable = false)
  private int offsetDays;

  @Column(name = "evidence_required", nullable = false)
  private boolean evidenceRequired;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected WorkflowTaskEntity() {
  }

  public WorkflowTaskEntity(
      UUID id,
      String code,
      String title,
      int sequenceNumber,
      int offsetDays,
      boolean evidenceRequired,
      Instant now
  ) {
    this.id = id;
    this.code = code;
    this.title = title;
    this.sequenceNumber = sequenceNumber;
    this.offsetDays = offsetDays;
    this.evidenceRequired = evidenceRequired;
    this.createdAt = now;
  }

  public UUID getId() {
    return id;
  }

  public WorkflowDefinitionEntity getWorkflowDefinition() {
    return workflowDefinition;
  }

  public String getCode() {
    return code;
  }

  public String getTitle() {
    return title;
  }

  public int getSequenceNumber() {
    return sequenceNumber;
  }

  public int getOffsetDays() {
    return offsetDays;
  }

  public boolean isEvidenceRequired() {
    return evidenceRequired;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  void attachTo(WorkflowDefinitionEntity workflowDefinition) {
    this.workflowDefinition = workflowDefinition;
  }
}
