package com.activityplatform.backend.farmer.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.farmer.service.FarmerBankDetailsService;
import com.activityplatform.backend.farmer.service.FarmerProfileCompletionService;
import com.activityplatform.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/farmer")
public class FarmerProfileController {
  private final FarmerBankDetailsService bankDetailsService;
  private final FarmerProfileCompletionService completionService;

  public FarmerProfileController(
      FarmerBankDetailsService bankDetailsService,
      FarmerProfileCompletionService completionService
  ) {
    this.bankDetailsService = bankDetailsService;
    this.completionService = completionService;
  }

  @GetMapping("/bank-details")
  @PreAuthorize("hasRole('FARMER')")
  ApiResponse<FarmerBankDetailsResponse> getBankDetails(Authentication authentication) {
    return ApiResponse.success(
        bankDetailsService.getCurrentFarmerBankDetails(CurrentUser.from(authentication))
    );
  }

  @PostMapping("/bank-details")
  @PreAuthorize("hasRole('FARMER')")
  ApiResponse<FarmerBankDetailsResponse> createOrUpdateBankDetails(
      Authentication authentication,
      @Valid @RequestBody FarmerBankDetailsRequest request
  ) {
    return ApiResponse.success(
        bankDetailsService.createOrUpdateCurrentFarmerBankDetails(
            CurrentUser.from(authentication),
            request
        )
    );
  }

  @PutMapping("/bank-details/{bankDetailsId}")
  @PreAuthorize("hasRole('FARMER')")
  ApiResponse<FarmerBankDetailsResponse> updateBankDetails(
      Authentication authentication,
      @PathVariable UUID bankDetailsId,
      @Valid @RequestBody FarmerBankDetailsRequest request
  ) {
    return ApiResponse.success(
        bankDetailsService.updateCurrentFarmerBankDetails(
            CurrentUser.from(authentication),
            bankDetailsId,
            request
        )
    );
  }

  @GetMapping("/profile/completion")
  @PreAuthorize("hasRole('FARMER')")
  ApiResponse<FarmerProfileCompletionResponse> completion(Authentication authentication) {
    return ApiResponse.success(
        completionService.getCurrentFarmerCompletion(CurrentUser.from(authentication))
    );
  }
}
