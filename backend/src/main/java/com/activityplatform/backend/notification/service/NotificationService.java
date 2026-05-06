package com.activityplatform.backend.notification.service;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.notification.api.CreateNotificationRequest;
import com.activityplatform.backend.notification.api.NotificationResponse;
import com.activityplatform.backend.notification.domain.NotificationEventEntity;
import com.activityplatform.backend.notification.domain.NotificationStatus;
import com.activityplatform.backend.notification.repository.NotificationEventRepository;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
  private final AuditEventService auditEventService;
  private final NotificationEventRepository notificationEventRepository;
  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;

  public NotificationService(
      AuditEventService auditEventService,
      NotificationEventRepository notificationEventRepository,
      TenantRepository tenantRepository,
      UserRepository userRepository
  ) {
    this.auditEventService = auditEventService;
    this.notificationEventRepository = notificationEventRepository;
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public Page<NotificationResponse> list(
      CurrentUser currentUser,
      NotificationStatus status,
      Pageable pageable
  ) {
    requireManager(currentUser);
    Page<NotificationEventEntity> notifications = status == null
        ? notificationEventRepository.findByTenantId(currentUser.tenantId(), pageable)
        : notificationEventRepository.findByTenantIdAndStatus(
            currentUser.tenantId(),
            status,
            pageable
        );

    return notifications.map(NotificationResponse::from);
  }

  @Transactional
  public NotificationResponse queue(CurrentUser currentUser, CreateNotificationRequest request) {
    requireManager(currentUser);
    TenantEntity tenant = requireTenant(currentUser);
    UserEntity recipient = request.recipientUserId() == null
        ? null
        : requireUser(currentUser, request.recipientUserId());
    NotificationEventEntity notification = notificationEventRepository.save(
        new NotificationEventEntity(
            UUID.randomUUID(),
            tenant,
            recipient,
            request.channel(),
            request.templateCode().trim(),
            request.payload(),
            Instant.now()
        )
    );

    auditEventService.record(
        tenant,
        actor(currentUser),
        "NOTIFICATION",
        notification.getId(),
        AuditAction.NOTIFICATION_QUEUED,
        Map.of(
            "channel", notification.getChannel().name(),
            "templateCode", notification.getTemplateCode(),
            "status", notification.getStatus().name()
        )
    );

    return NotificationResponse.from(notification);
  }

  @Transactional
  public NotificationResponse updateStatus(
      CurrentUser currentUser,
      UUID notificationId,
      NotificationStatus status
  ) {
    requireManager(currentUser);
    NotificationEventEntity notification = notificationEventRepository
        .findByIdAndTenantId(notificationId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Notification not found."));
    NotificationStatus previousStatus = notification.getStatus();
    notification.updateStatus(status, Instant.now());
    NotificationEventEntity savedNotification = notificationEventRepository.save(notification);

    auditEventService.record(
        savedNotification.getTenant(),
        actor(currentUser),
        "NOTIFICATION",
        savedNotification.getId(),
        AuditAction.NOTIFICATION_STATUS_CHANGED,
        Map.of(
            "previousStatus", previousStatus.name(),
            "status", savedNotification.getStatus().name()
        )
    );

    return NotificationResponse.from(savedNotification);
  }

  private TenantEntity requireTenant(CurrentUser currentUser) {
    return tenantRepository.findById(currentUser.tenantId())
        .orElseThrow(() -> notFound("Tenant not found."));
  }

  private UserEntity requireUser(CurrentUser currentUser, UUID userId) {
    return userRepository.findByIdAndTenantId(userId, currentUser.tenantId())
        .orElseThrow(() -> notFound("Recipient user not found."));
  }

  private void requireManager(CurrentUser currentUser) {
    if (!currentUser.hasAnyRole(Role.ADMIN, Role.SUPERVISOR)) {
      throw new ApplicationException(
          ErrorCode.ACCESS_DENIED,
          "Only admins and supervisors can manage notifications.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  private UserEntity actor(CurrentUser currentUser) {
    return userRepository.findById(currentUser.userId()).orElse(null);
  }

  private ApplicationException notFound(String message) {
    return new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, message, HttpStatus.NOT_FOUND);
  }
}
