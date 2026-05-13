package com.activityplatform.backend.role.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.role.service.RoleManagementService;
import com.activityplatform.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RoleController {
  private final RoleManagementService roleManagementService;

  public RoleController(RoleManagementService roleManagementService) {
    this.roleManagementService = roleManagementService;
  }

  @GetMapping("/roles")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER')")
  ApiResponse<List<RoleResponse>> listRoles(Authentication authentication) {
    return ApiResponse.success(roleManagementService.listRoles(CurrentUser.from(authentication)));
  }

  @GetMapping("/users/{userId}/roles")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER')")
  ApiResponse<UserRolesResponse> getUserRoles(
      Authentication authentication,
      @PathVariable UUID userId
  ) {
    return ApiResponse.success(
        roleManagementService.getUserRoles(CurrentUser.from(authentication), userId));
  }

  @PutMapping("/users/{userId}/roles")
  @PreAuthorize("hasRole('ADMIN')")
  ApiResponse<UserRolesResponse> updateUserRoles(
      Authentication authentication,
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateUserRolesRequest request
  ) {
    return ApiResponse.success(roleManagementService.updateUserRoles(
        CurrentUser.from(authentication),
        userId,
        request
    ));
  }
}
