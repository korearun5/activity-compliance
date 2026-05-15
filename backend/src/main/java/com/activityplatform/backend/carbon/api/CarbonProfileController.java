package com.activityplatform.backend.carbon.api;

import com.activityplatform.backend.carbon.domain.CarbonRecordStatus;
import com.activityplatform.backend.carbon.service.CarbonProfileService;
import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.common.api.PageResponse;
import com.activityplatform.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/carbon")
public class CarbonProfileController {
  private final CarbonProfileService carbonProfileService;

  public CarbonProfileController(CarbonProfileService carbonProfileService) {
    this.carbonProfileService = carbonProfileService;
  }

  @GetMapping("/profiles")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<PageResponse<CarbonProfileResponse>> listProfiles(
      Authentication authentication,
      @RequestParam(required = false) CarbonRecordStatus status,
      @PageableDefault(size = 20, page = 0) Pageable pageable
  ) {
    return ApiResponse.success(PageResponse.from(
        carbonProfileService.list(CurrentUser.from(authentication), status, pageable)));
  }

  @GetMapping("/profiles/me")
  ApiResponse<CarbonProfileResponse> me(Authentication authentication) {
    return ApiResponse.success(carbonProfileService.me(CurrentUser.from(authentication)));
  }

  @GetMapping("/profiles/{profileId}")
  ApiResponse<CarbonProfileResponse> getProfile(
      Authentication authentication,
      @PathVariable UUID profileId
  ) {
    return ApiResponse.success(carbonProfileService.get(
        CurrentUser.from(authentication),
        profileId
    ));
  }

  @PostMapping("/profiles")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<CarbonProfileResponse> createProfile(
      Authentication authentication,
      @Valid @RequestBody CarbonProfileRequest request
  ) {
    return ApiResponse.success(carbonProfileService.create(
        CurrentUser.from(authentication),
        request
    ));
  }

  @PutMapping("/profiles/{profileId}")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<CarbonProfileResponse> updateProfile(
      Authentication authentication,
      @PathVariable UUID profileId,
      @Valid @RequestBody CarbonProfileRequest request
  ) {
    return ApiResponse.success(carbonProfileService.update(
        CurrentUser.from(authentication),
        profileId,
        request
    ));
  }

  @GetMapping("/profiles/{profileId}/plots")
  ApiResponse<List<CarbonFarmPlotResponse>> listPlots(
      Authentication authentication,
      @PathVariable UUID profileId
  ) {
    return ApiResponse.success(carbonProfileService.listPlots(
        CurrentUser.from(authentication),
        profileId
    ));
  }

  @PostMapping("/profiles/{profileId}/plots")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<CarbonFarmPlotResponse> createPlot(
      Authentication authentication,
      @PathVariable UUID profileId,
      @Valid @RequestBody CarbonFarmPlotRequest request
  ) {
    return ApiResponse.success(carbonProfileService.createPlot(
        CurrentUser.from(authentication),
        profileId,
        request
    ));
  }

  @PutMapping("/plots/{plotId}")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<CarbonFarmPlotResponse> updatePlot(
      Authentication authentication,
      @PathVariable UUID plotId,
      @Valid @RequestBody CarbonFarmPlotRequest request
  ) {
    return ApiResponse.success(carbonProfileService.updatePlot(
        CurrentUser.from(authentication),
        plotId,
        request
    ));
  }

  @GetMapping("/profiles/{profileId}/soil-profiles")
  ApiResponse<List<CarbonSoilProfileResponse>> listSoilProfiles(
      Authentication authentication,
      @PathVariable UUID profileId
  ) {
    return ApiResponse.success(carbonProfileService.listSoilProfiles(
        CurrentUser.from(authentication),
        profileId
    ));
  }

  @PostMapping("/profiles/{profileId}/soil-profiles")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<CarbonSoilProfileResponse> createSoilProfile(
      Authentication authentication,
      @PathVariable UUID profileId,
      @Valid @RequestBody CarbonSoilProfileRequest request
  ) {
    return ApiResponse.success(carbonProfileService.createSoilProfile(
        CurrentUser.from(authentication),
        profileId,
        request
    ));
  }

  @PutMapping("/soil-profiles/{soilProfileId}")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<CarbonSoilProfileResponse> updateSoilProfile(
      Authentication authentication,
      @PathVariable UUID soilProfileId,
      @Valid @RequestBody CarbonSoilProfileRequest request
  ) {
    return ApiResponse.success(carbonProfileService.updateSoilProfile(
        CurrentUser.from(authentication),
        soilProfileId,
        request
    ));
  }
}
