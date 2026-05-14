package com.activityplatform.backend.user.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.common.api.PageResponse;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER')")
  ApiResponse<PageResponse<UserResponse>> list(
      Authentication authentication,
      @PageableDefault(size = 20, page = 0) Pageable pageable
  ) {
    return ApiResponse.success(
        PageResponse.from(userService.list(CurrentUser.from(authentication), pageable)));
  }

  @GetMapping("/me")
  ApiResponse<UserResponse> me(Authentication authentication) {
    return ApiResponse.success(userService.me(CurrentUser.from(authentication)));
  }

  @GetMapping("/{userId}")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER')")
  ApiResponse<UserResponse> get(Authentication authentication, @PathVariable UUID userId) {
    return ApiResponse.success(userService.get(CurrentUser.from(authentication), userId));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER')")
  ApiResponse<UserResponse> create(
      Authentication authentication,
      @Valid @RequestBody CreateUserRequest request
  ) {
    return ApiResponse.success(userService.createStaffUser(CurrentUser.from(authentication), request));
  }

  @PutMapping("/{userId}")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER')")
  ApiResponse<UserResponse> update(
      Authentication authentication,
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateUserRequest request
  ) {
    return ApiResponse.success(userService.update(CurrentUser.from(authentication), userId, request));
  }

  @PatchMapping("/{userId}/status")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER')")
  ApiResponse<UserResponse> updateStatus(
      Authentication authentication,
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateUserStatusRequest request
  ) {
    return ApiResponse.success(
        userService.updateStatus(CurrentUser.from(authentication), userId, request.status()));
  }
}
