package com.activityplatform.backend.activity.api;

import com.activityplatform.backend.activity.domain.ActivityStatus;
import com.activityplatform.backend.activity.service.ActivityService;
import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.common.api.PageResponse;
import com.activityplatform.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/v1/activities")
public class ActivityController {
  private final ActivityService activityService;

  public ActivityController(ActivityService activityService) {
    this.activityService = activityService;
  }

  @GetMapping
  ApiResponse<PageResponse<ActivityResponse>> list(
      Authentication authentication,
      @RequestParam(required = false) ActivityStatus status,
      @PageableDefault(size = 20, page = 0) Pageable pageable) {
    return ApiResponse.success(
        PageResponse.from(activityService.list(CurrentUser.from(authentication), status, pageable)));
  }

  @GetMapping("/{activityId}")
  ApiResponse<ActivityResponse> get(
      Authentication authentication,
      @PathVariable UUID activityId) {
    return ApiResponse.success(activityService.get(CurrentUser.from(authentication), activityId));
  }

  @PostMapping
  ApiResponse<ActivityResponse> start(
      Authentication authentication,
      @Valid @RequestBody StartActivityRequest request) {
    return ApiResponse.success(activityService.start(CurrentUser.from(authentication), request));
  }

  @PatchMapping("/{activityId}/tasks/{activityTaskId}/status")
  ApiResponse<ActivityResponse> updateTaskStatus(
      Authentication authentication,
      @PathVariable UUID activityId,
      @PathVariable UUID activityTaskId,
      @Valid @RequestBody UpdateActivityTaskStatusRequest request) {
    return ApiResponse.success(activityService.updateTaskStatus(
        CurrentUser.from(authentication),
        activityId,
        activityTaskId,
        request.status()));
  }
}
