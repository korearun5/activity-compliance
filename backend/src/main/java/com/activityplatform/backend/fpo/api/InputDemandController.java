package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.fpo.domain.FarmRecordStatus;
import com.activityplatform.backend.fpo.service.InputDemandService;
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
public class InputDemandController {
  private final InputDemandService inputDemandService;

  public InputDemandController(InputDemandService inputDemandService) {
    this.inputDemandService = inputDemandService;
  }

  @GetMapping("/inputs")
  @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
  ApiResponse<List<InputCatalogResponse>> listInputs(Authentication authentication) {
    return ApiResponse.success(
        inputDemandService.listInputs(CurrentUser.from(authentication)));
  }

  @PostMapping("/inputs")
  @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
  ApiResponse<InputCatalogResponse> createInput(
      Authentication authentication,
      @Valid @RequestBody InputCatalogRequest request
  ) {
    return ApiResponse.success(inputDemandService.createInput(
        CurrentUser.from(authentication),
        request
    ));
  }

  @PutMapping("/inputs/{inputId}")
  @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
  ApiResponse<InputCatalogResponse> updateInput(
      Authentication authentication,
      @PathVariable UUID inputId,
      @Valid @RequestBody InputCatalogRequest request
  ) {
    return ApiResponse.success(inputDemandService.updateInput(
        CurrentUser.from(authentication),
        inputId,
        request
    ));
  }

  @PatchMapping("/inputs/{inputId}/status")
  @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
  ApiResponse<InputCatalogResponse> updateInputStatus(
      Authentication authentication,
      @PathVariable UUID inputId,
      @Valid @RequestBody UpdateFarmRecordStatusRequest request
  ) {
    return ApiResponse.success(inputDemandService.updateInputStatus(
        CurrentUser.from(authentication),
        inputId,
        request.status()
    ));
  }

  @GetMapping("/input-rules")
  @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
  ApiResponse<List<CropInputRuleResponse>> listRules(
      Authentication authentication,
      @RequestParam(required = false) UUID cropId,
      @RequestParam(required = false) UUID inputId,
      @RequestParam(required = false) FarmRecordStatus status
  ) {
    return ApiResponse.success(inputDemandService.listRules(
        CurrentUser.from(authentication),
        cropId,
        inputId,
        status
    ));
  }

  @PostMapping("/input-rules")
  @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
  ApiResponse<CropInputRuleResponse> createRule(
      Authentication authentication,
      @Valid @RequestBody CropInputRuleRequest request
  ) {
    return ApiResponse.success(inputDemandService.createRule(
        CurrentUser.from(authentication),
        request
    ));
  }

  @PutMapping("/input-rules/{ruleId}")
  @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
  ApiResponse<CropInputRuleResponse> updateRule(
      Authentication authentication,
      @PathVariable UUID ruleId,
      @Valid @RequestBody CropInputRuleRequest request
  ) {
    return ApiResponse.success(inputDemandService.updateRule(
        CurrentUser.from(authentication),
        ruleId,
        request
    ));
  }

  @PatchMapping("/input-rules/{ruleId}/status")
  @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
  ApiResponse<CropInputRuleResponse> updateRuleStatus(
      Authentication authentication,
      @PathVariable UUID ruleId,
      @Valid @RequestBody UpdateFarmRecordStatusRequest request
  ) {
    return ApiResponse.success(inputDemandService.updateRuleStatus(
        CurrentUser.from(authentication),
        ruleId,
        request.status()
    ));
  }

  @PostMapping("/demand-estimates/run")
  @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
  ApiResponse<InputDemandRunResponse> runDemandEstimate(
      Authentication authentication,
      @Valid @RequestBody InputDemandRunRequest request
  ) {
    return ApiResponse.success(inputDemandService.runDemandEstimate(
        CurrentUser.from(authentication),
        request
    ));
  }

  @GetMapping("/demand-estimates")
  @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
  ApiResponse<List<InputDemandEstimateResponse>> listEstimates(
      Authentication authentication,
      @RequestParam(required = false) UUID seasonId,
      @RequestParam(required = false) UUID cropId,
      @RequestParam(required = false) String village
  ) {
    return ApiResponse.success(inputDemandService.listEstimates(
        CurrentUser.from(authentication),
        seasonId,
        cropId,
        village
    ));
  }

  @GetMapping("/demand-estimates/summary")
  @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
  ApiResponse<InputDemandSummaryResponse> summarize(
      Authentication authentication,
      @RequestParam(required = false) UUID seasonId,
      @RequestParam(required = false) UUID cropId,
      @RequestParam(required = false) String village
  ) {
    return ApiResponse.success(inputDemandService.summarize(
        CurrentUser.from(authentication),
        seasonId,
        cropId,
        village
    ));
  }
}
