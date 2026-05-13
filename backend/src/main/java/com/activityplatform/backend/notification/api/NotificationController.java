package com.activityplatform.backend.notification.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.common.api.PageResponse;
import com.activityplatform.backend.notification.domain.NotificationStatus;
import com.activityplatform.backend.notification.service.NotificationService;
import com.activityplatform.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER')")
  ApiResponse<PageResponse<NotificationResponse>> list(
      Authentication authentication,
      @RequestParam(required = false) NotificationStatus status,
      @PageableDefault(size = 20, page = 0, sort = "queuedAt", direction = Sort.Direction.DESC)
      Pageable pageable
  ) {
    return ApiResponse.success(PageResponse.from(notificationService.list(
        CurrentUser.from(authentication),
        status,
        pageable
    )));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER')")
  ApiResponse<NotificationResponse> queue(
      Authentication authentication,
      @Valid @RequestBody CreateNotificationRequest request
  ) {
    return ApiResponse.success(notificationService.queue(CurrentUser.from(authentication), request));
  }

  @PatchMapping("/{notificationId}/status")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER')")
  ApiResponse<NotificationResponse> updateStatus(
      Authentication authentication,
      @PathVariable UUID notificationId,
      @Valid @RequestBody UpdateNotificationStatusRequest request
  ) {
    return ApiResponse.success(notificationService.updateStatus(
        CurrentUser.from(authentication),
        notificationId,
        request.status()
    ));
  }
}
