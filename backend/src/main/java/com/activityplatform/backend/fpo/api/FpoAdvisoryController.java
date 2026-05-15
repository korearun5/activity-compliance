package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.fpo.domain.AdvisoryStatus;
import com.activityplatform.backend.fpo.domain.AdvisoryTargetType;
import com.activityplatform.backend.fpo.domain.AdvisoryCategory;
import com.activityplatform.backend.fpo.service.FpoAdvisoryService;
import com.activityplatform.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/v1/fpo/advisories")
public class FpoAdvisoryController {
  private final FpoAdvisoryService advisoryService;

  public FpoAdvisoryController(FpoAdvisoryService advisoryService) {
    this.advisoryService = advisoryService;
  }

  @GetMapping
  ApiResponse<List<FpoAdvisoryResponse>> list(
      Authentication authentication,
      @RequestParam(required = false) AdvisoryStatus status,
      @RequestParam(required = false) AdvisoryCategory category,
      @RequestParam(required = false) AdvisoryTargetType targetType,
      @RequestParam(required = false) UUID cropId,
      @RequestParam(required = false) UUID seasonId
  ) {
    return ApiResponse.success(advisoryService.list(
        CurrentUser.from(authentication),
        status,
        category,
        targetType,
        cropId,
        seasonId
    ));
  }

  @GetMapping("/{advisoryId}")
  ApiResponse<FpoAdvisoryResponse> get(
      Authentication authentication,
      @PathVariable UUID advisoryId
  ) {
    return ApiResponse.success(advisoryService.get(
        CurrentUser.from(authentication),
        advisoryId
    ));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER')")
  ApiResponse<FpoAdvisoryResponse> create(
      Authentication authentication,
      @Valid @RequestBody FpoAdvisoryRequest request
  ) {
    return ApiResponse.success(advisoryService.create(
        CurrentUser.from(authentication),
        request
    ));
  }

  @PatchMapping("/{advisoryId}/status")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER')")
  ApiResponse<FpoAdvisoryResponse> updateStatus(
      Authentication authentication,
      @PathVariable UUID advisoryId,
      @Valid @RequestBody UpdateFpoAdvisoryStatusRequest request
  ) {
    return ApiResponse.success(advisoryService.updateStatus(
        CurrentUser.from(authentication),
        advisoryId,
        request.status()
    ));
  }
}
