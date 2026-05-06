package com.activityplatform.backend.workflow.domain;

import com.activityplatform.backend.auth.domain.TenantEntity;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workflow_definitions")
public class WorkflowDefinitionEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @Column(nullable = false)
  private String code;

  @Column(nullable = false)
  private String name;

  @Column(name = "domain_key")
  private String domainKey;

  @Column(name = "duration_days", nullable = false)
  private int durationDays;

  @Column(nullable = false)
  private int version;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private WorkflowDefinitionStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "workflowDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sequenceNumber ASC")
  private List<WorkflowTaskEntity> tasks = new ArrayList<>();

  protected WorkflowDefinitionEntity() {
  }

  public WorkflowDefinitionEntity(
      UUID id,
      TenantEntity tenant,
      String code,
      String name,
      String domainKey,
      int durationDays,
      int version,
      WorkflowDefinitionStatus status,
      Instant now
  ) {
    this.id = id;
    this.tenant = tenant;
    this.code = code;
    this.name = name;
    this.domainKey = domainKey;
    this.durationDays = durationDays;
    this.version = version;
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

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getDomainKey() {
    return domainKey;
  }

  public int getDurationDays() {
    return durationDays;
  }

  public int getVersion() {
    return version;
  }

  public WorkflowDefinitionStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public List<WorkflowTaskEntity> getTasks() {
    return tasks.stream()
        .sorted(Comparator.comparingInt(WorkflowTaskEntity::getSequenceNumber))
        .toList();
  }

  public void updateDetails(
      String name,
      String domainKey,
      int durationDays,
      WorkflowDefinitionStatus status,
      Instant now
  ) {
    this.name = name;
    this.domainKey = domainKey;
    this.durationDays = durationDays;
    this.status = status;
    this.updatedAt = now;
  }

  public void updateStatus(WorkflowDefinitionStatus status, Instant now) {
    this.status = status;
    this.updatedAt = now;
  }

  public void replaceTasks(List<WorkflowTaskEntity> nextTasks, Instant now) {
    tasks.clear();
    nextTasks.forEach(this::addTask);
    this.updatedAt = now;
  }

  public void addTask(WorkflowTaskEntity task) {
    task.attachTo(this);
    tasks.add(task);
  }
}
