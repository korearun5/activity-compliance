package com.activityplatform.backend.farmer.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.farmer.service.FarmerService;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/farmers")
public class FarmerParticipantController {
  private final FarmerService farmerService;

  public FarmerParticipantController(FarmerService farmerService) {
    this.farmerService = farmerService;
  }

  @GetMapping("/participants")
  ApiResponse<List<FarmerParticipantResponse>> listParticipants(
      Authentication authentication
  ) {
    CurrentUser currentUser = CurrentUser.from(authentication);
    requireStaff(currentUser);
    return ApiResponse.success(farmerService.findParticipants(currentUser.tenantId())
        .stream()
        .map(FarmerParticipantResponse::from)
        .toList());
  }

  private void requireStaff(CurrentUser currentUser) {
    if (!currentUser.hasAnyRole(Role.ADMIN, Role.FPO_MANAGER, Role.FIELD_COORDINATOR)) {
      throw new ApplicationException(
          ErrorCode.ACCESS_DENIED,
          "Only staff users can list activity participants.",
          HttpStatus.FORBIDDEN
      );
    }
  }
}
