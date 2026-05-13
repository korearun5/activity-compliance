package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.fpo.service.FarmAssetService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fpo")
public class FarmAssetController {
  private final FarmAssetService farmAssetService;

  public FarmAssetController(FarmAssetService farmAssetService) {
    this.farmAssetService = farmAssetService;
  }

  @GetMapping("/members/{memberId}/landholdings")
  ApiResponse<List<FarmLandholdingResponse>> listLandholdings(
      Authentication authentication,
      @PathVariable UUID memberId
  ) {
    return ApiResponse.success(
        farmAssetService.listLandholdings(CurrentUser.from(authentication), memberId));
  }

  @PostMapping("/members/{memberId}/landholdings")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<FarmLandholdingResponse> createLandholding(
      Authentication authentication,
      @PathVariable UUID memberId,
      @Valid @RequestBody CreateFarmLandholdingRequest request
  ) {
    return ApiResponse.success(farmAssetService.createLandholding(
        CurrentUser.from(authentication),
        memberId,
        request
    ));
  }

  @PutMapping("/landholdings/{landholdingId}")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<FarmLandholdingResponse> updateLandholding(
      Authentication authentication,
      @PathVariable UUID landholdingId,
      @Valid @RequestBody UpdateFarmLandholdingRequest request
  ) {
    return ApiResponse.success(farmAssetService.updateLandholding(
        CurrentUser.from(authentication),
        landholdingId,
        request
    ));
  }

  @PatchMapping("/landholdings/{landholdingId}/status")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<FarmLandholdingResponse> updateLandholdingStatus(
      Authentication authentication,
      @PathVariable UUID landholdingId,
      @Valid @RequestBody UpdateFarmRecordStatusRequest request
  ) {
    return ApiResponse.success(farmAssetService.updateLandholdingStatus(
        CurrentUser.from(authentication),
        landholdingId,
        request.status()
    ));
  }

  @GetMapping("/members/{memberId}/plots")
  ApiResponse<List<FarmPlotResponse>> listPlots(
      Authentication authentication,
      @PathVariable UUID memberId
  ) {
    return ApiResponse.success(
        farmAssetService.listPlots(CurrentUser.from(authentication), memberId));
  }

  @PostMapping("/members/{memberId}/plots")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<FarmPlotResponse> createPlot(
      Authentication authentication,
      @PathVariable UUID memberId,
      @Valid @RequestBody CreateFarmPlotRequest request
  ) {
    return ApiResponse.success(farmAssetService.createPlot(
        CurrentUser.from(authentication),
        memberId,
        request
    ));
  }

  @PutMapping("/plots/{plotId}")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<FarmPlotResponse> updatePlot(
      Authentication authentication,
      @PathVariable UUID plotId,
      @Valid @RequestBody UpdateFarmPlotRequest request
  ) {
    return ApiResponse.success(farmAssetService.updatePlot(
        CurrentUser.from(authentication),
        plotId,
        request
    ));
  }

  @PatchMapping("/plots/{plotId}/status")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<FarmPlotResponse> updatePlotStatus(
      Authentication authentication,
      @PathVariable UUID plotId,
      @Valid @RequestBody UpdateFarmRecordStatusRequest request
  ) {
    return ApiResponse.success(farmAssetService.updatePlotStatus(
        CurrentUser.from(authentication),
        plotId,
        request.status()
    ));
  }
}
