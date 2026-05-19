package com.activityplatform.backend.farmer.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.farmer.service.FarmerBankDetailsService;
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
@RequestMapping("/api/v1/admin/bank-details")
public class AdminBankDetailsController {
  private final FarmerBankDetailsService bankDetailsService;

  public AdminBankDetailsController(FarmerBankDetailsService bankDetailsService) {
    this.bankDetailsService = bankDetailsService;
  }

  @GetMapping("/pending")
  @PreAuthorize("hasRole('ADMIN')")
  ApiResponse<List<FarmerBankDetailsResponse>> listPending(Authentication authentication) {
    return ApiResponse.success(bankDetailsService.listPendingForAdmin(
        CurrentUser.from(authentication)
    ));
  }

  @PutMapping("/{bankDetailsId}/verify")
  @PreAuthorize("hasRole('ADMIN')")
  ApiResponse<FarmerBankDetailsResponse> verify(
      Authentication authentication,
      @PathVariable UUID bankDetailsId,
      @Valid @RequestBody FarmerBankDetailsVerificationRequest request
  ) {
    return ApiResponse.success(bankDetailsService.verifyForAdmin(
        CurrentUser.from(authentication),
        bankDetailsId,
        request
    ));
  }
}
