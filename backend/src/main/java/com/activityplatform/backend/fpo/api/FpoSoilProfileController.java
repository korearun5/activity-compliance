package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.fpo.service.FpoSoilProfileService;
import com.activityplatform.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fpo")
public class FpoSoilProfileController {
  private final FpoSoilProfileService soilProfileService;

  public FpoSoilProfileController(FpoSoilProfileService soilProfileService) {
    this.soilProfileService = soilProfileService;
  }

  @GetMapping("/members/{memberId}/soil-profiles")
  ApiResponse<List<FpoSoilProfileResponse>> list(
      Authentication authentication,
      @PathVariable UUID memberId
  ) {
    return ApiResponse.success(soilProfileService.list(CurrentUser.from(authentication), memberId));
  }

  @PostMapping("/members/{memberId}/soil-profiles")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<FpoSoilProfileResponse> create(
      Authentication authentication,
      @PathVariable UUID memberId,
      @Valid @RequestBody FpoSoilProfileRequest request
  ) {
    return ApiResponse.success(
        soilProfileService.create(CurrentUser.from(authentication), memberId, request));
  }

  @PutMapping("/soil-profiles/{soilProfileId}")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<FpoSoilProfileResponse> update(
      Authentication authentication,
      @PathVariable UUID soilProfileId,
      @Valid @RequestBody FpoSoilProfileRequest request
  ) {
    return ApiResponse.success(
        soilProfileService.update(CurrentUser.from(authentication), soilProfileId, request));
  }
}
