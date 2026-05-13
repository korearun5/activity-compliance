package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.fpo.domain.CropPlanStatus;
import com.activityplatform.backend.fpo.service.CropPlanningService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fpo")
public class CropPlanningController {
  private final CropPlanningService cropPlanningService;

  public CropPlanningController(CropPlanningService cropPlanningService) {
    this.cropPlanningService = cropPlanningService;
  }

  @GetMapping("/crops")
  ApiResponse<List<CropCatalogResponse>> listCrops(Authentication authentication) {
    return ApiResponse.success(
        cropPlanningService.listCrops(CurrentUser.from(authentication)));
  }

  @PostMapping("/crops")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<CropCatalogResponse> createCrop(
      Authentication authentication,
      @Valid @RequestBody CropCatalogRequest request
  ) {
    return ApiResponse.success(cropPlanningService.createCrop(
        CurrentUser.from(authentication),
        request
    ));
  }

  @PutMapping("/crops/{cropId}")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<CropCatalogResponse> updateCrop(
      Authentication authentication,
      @PathVariable UUID cropId,
      @Valid @RequestBody CropCatalogRequest request
  ) {
    return ApiResponse.success(cropPlanningService.updateCrop(
        CurrentUser.from(authentication),
        cropId,
        request
    ));
  }

  @PatchMapping("/crops/{cropId}/status")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<CropCatalogResponse> updateCropStatus(
      Authentication authentication,
      @PathVariable UUID cropId,
      @Valid @RequestBody UpdateFarmRecordStatusRequest request
  ) {
    return ApiResponse.success(cropPlanningService.updateCropStatus(
        CurrentUser.from(authentication),
        cropId,
        request.status()
    ));
  }

  @GetMapping("/seasons")
  ApiResponse<List<CropSeasonResponse>> listSeasons(Authentication authentication) {
    return ApiResponse.success(
        cropPlanningService.listSeasons(CurrentUser.from(authentication)));
  }

  @PostMapping("/seasons")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<CropSeasonResponse> createSeason(
      Authentication authentication,
      @Valid @RequestBody CropSeasonRequest request
  ) {
    return ApiResponse.success(cropPlanningService.createSeason(
        CurrentUser.from(authentication),
        request
    ));
  }

  @PutMapping("/seasons/{seasonId}")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<CropSeasonResponse> updateSeason(
      Authentication authentication,
      @PathVariable UUID seasonId,
      @Valid @RequestBody CropSeasonRequest request
  ) {
    return ApiResponse.success(cropPlanningService.updateSeason(
        CurrentUser.from(authentication),
        seasonId,
        request
    ));
  }

  @PatchMapping("/seasons/{seasonId}/status")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<CropSeasonResponse> updateSeasonStatus(
      Authentication authentication,
      @PathVariable UUID seasonId,
      @Valid @RequestBody UpdateFarmRecordStatusRequest request
  ) {
    return ApiResponse.success(cropPlanningService.updateSeasonStatus(
        CurrentUser.from(authentication),
        seasonId,
        request.status()
    ));
  }

  @GetMapping("/members/{memberId}/crop-history")
  ApiResponse<List<CropHistoryResponse>> listCropHistory(
      Authentication authentication,
      @PathVariable UUID memberId
  ) {
    return ApiResponse.success(cropPlanningService.listCropHistory(
        CurrentUser.from(authentication),
        memberId
    ));
  }

  @PostMapping("/members/{memberId}/crop-history")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<CropHistoryResponse> createCropHistory(
      Authentication authentication,
      @PathVariable UUID memberId,
      @Valid @RequestBody CropHistoryRequest request
  ) {
    return ApiResponse.success(cropPlanningService.createCropHistory(
        CurrentUser.from(authentication),
        memberId,
        request
    ));
  }

  @PutMapping("/crop-history/{historyId}")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<CropHistoryResponse> updateCropHistory(
      Authentication authentication,
      @PathVariable UUID historyId,
      @Valid @RequestBody CropHistoryRequest request
  ) {
    return ApiResponse.success(cropPlanningService.updateCropHistory(
        CurrentUser.from(authentication),
        historyId,
        request
    ));
  }

  @GetMapping("/crop-plans")
  ApiResponse<List<CropPlanResponse>> listCropPlans(
      Authentication authentication,
      @RequestParam(required = false) UUID memberId,
      @RequestParam(required = false) UUID cropId,
      @RequestParam(required = false) UUID seasonId,
      @RequestParam(required = false) CropPlanStatus status
  ) {
    return ApiResponse.success(cropPlanningService.listCropPlans(
        CurrentUser.from(authentication),
        memberId,
        cropId,
        seasonId,
        status
    ));
  }

  @GetMapping("/crop-plans/{planId}")
  ApiResponse<CropPlanResponse> getCropPlan(
      Authentication authentication,
      @PathVariable UUID planId
  ) {
    return ApiResponse.success(cropPlanningService.getCropPlan(
        CurrentUser.from(authentication),
        planId
    ));
  }

  @PostMapping("/crop-plans")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<CropPlanResponse> createCropPlan(
      Authentication authentication,
      @Valid @RequestBody CropPlanRequest request
  ) {
    return ApiResponse.success(cropPlanningService.createCropPlan(
        CurrentUser.from(authentication),
        request
    ));
  }

  @PutMapping("/crop-plans/{planId}")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<CropPlanResponse> updateCropPlan(
      Authentication authentication,
      @PathVariable UUID planId,
      @Valid @RequestBody CropPlanRequest request
  ) {
    return ApiResponse.success(cropPlanningService.updateCropPlan(
        CurrentUser.from(authentication),
        planId,
        request
    ));
  }

  @PatchMapping("/crop-plans/{planId}/status")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER','FIELD_COORDINATOR')")
  ApiResponse<CropPlanResponse> updateCropPlanStatus(
      Authentication authentication,
      @PathVariable UUID planId,
      @Valid @RequestBody UpdateCropPlanStatusRequest request
  ) {
    return ApiResponse.success(cropPlanningService.updateCropPlanStatus(
        CurrentUser.from(authentication),
        planId,
        request.status()
    ));
  }
}
