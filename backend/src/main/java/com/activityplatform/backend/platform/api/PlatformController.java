package com.activityplatform.backend.platform.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.service.TenantModuleService;
import com.activityplatform.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform")
public class PlatformController {
  private final TenantModuleService tenantModuleService;

  public PlatformController(TenantModuleService tenantModuleService) {
    this.tenantModuleService = tenantModuleService;
  }

  @GetMapping("/modules")
  ApiResponse<List<PlatformModuleResponse>> modules() {
    return ApiResponse.success(tenantModuleService.listCatalog());
  }

  @GetMapping("/modules/enabled")
  ApiResponse<EnabledModulesResponse> enabledModules(Authentication authentication) {
    List<String> modules = tenantModuleService
        .findEnabledModuleCodes(CurrentUser.from(authentication).tenantId())
        .stream()
        .map(Enum::name)
        .toList();
    return ApiResponse.success(new EnabledModulesResponse(modules));
  }

  @GetMapping("/module-subscriptions")
  @PreAuthorize("hasRole('ADMIN')")
  ApiResponse<List<TenantModuleSubscriptionResponse>> moduleSubscriptions(
      Authentication authentication
  ) {
    return ApiResponse.success(
        tenantModuleService.listSubscriptions(CurrentUser.from(authentication)));
  }

  @PutMapping("/module-subscriptions/{moduleCode}")
  @PreAuthorize("hasRole('ADMIN')")
  ApiResponse<TenantModuleSubscriptionResponse> updateModuleSubscription(
      Authentication authentication,
      @PathVariable String moduleCode,
      @Valid @RequestBody TenantModuleSubscriptionRequest request
  ) {
    return ApiResponse.success(tenantModuleService.updateSubscription(
        CurrentUser.from(authentication),
        parseModuleCode(moduleCode),
        request.status(),
        request.expiresAt()
    ));
  }

  private ModuleCode parseModuleCode(String moduleCode) {
    try {
      return ModuleCode.from(moduleCode);
    } catch (IllegalArgumentException exception) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_FAILED,
          "Unsupported module code.",
          HttpStatus.BAD_REQUEST
      );
    }
  }
}
