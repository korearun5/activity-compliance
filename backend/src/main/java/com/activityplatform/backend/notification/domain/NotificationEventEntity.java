package com.activityplatform.backend.notification.domain;

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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notification_events")
public class NotificationEventEntity {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipient_user_id")
  private UserEntity recipient;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationChannel channel;

  @Column(name = "template_code", nullable = false)
  private String templateCode;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> payload = new LinkedHashMap<>();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationStatus status;

  @Column(name = "queued_at", nullable = false)
  private Instant queuedAt;

  @Column(name = "sent_at")
  private Instant sentAt;

  protected NotificationEventEntity() {
  }

  public NotificationEventEntity(
      UUID id,
      TenantEntity tenant,
      UserEntity recipient,
      NotificationChannel channel,
      String templateCode,
      Map<String, Object> payload,
      Instant queuedAt
  ) {
    this.id = id;
    this.tenant = tenant;
    this.recipient = recipient;
    this.channel = channel;
    this.templateCode = templateCode;
    this.payload = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
    this.status = NotificationStatus.QUEUED;
    this.queuedAt = queuedAt;
  }

  public UUID getId() {
    return id;
  }

  public TenantEntity getTenant() {
    return tenant;
  }

  public UserEntity getRecipient() {
    return recipient;
  }

  public NotificationChannel getChannel() {
    return channel;
  }

  public String getTemplateCode() {
    return templateCode;
  }

  public Map<String, Object> getPayload() {
    return payload;
  }

  public NotificationStatus getStatus() {
    return status;
  }

  public Instant getQueuedAt() {
    return queuedAt;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public void updateStatus(NotificationStatus status, Instant now) {
    this.status = status;
    this.sentAt = status == NotificationStatus.SENT ? now : null;
  }
}
