package com.activityplatform.backend.farmer.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.farmer.service.FarmerDocumentService;
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
@RequestMapping("/api/v1/admin/documents")
public class AdminFarmerDocumentController {
  private final FarmerDocumentService documentService;

  public AdminFarmerDocumentController(FarmerDocumentService documentService) {
    this.documentService = documentService;
  }

  @GetMapping("/pending")
  @PreAuthorize("hasRole('ADMIN')")
  ApiResponse<List<FarmerDocumentResponse>> listPending(Authentication authentication) {
    return ApiResponse.success(documentService.listPendingForAdmin(
        CurrentUser.from(authentication)
    ));
  }

  @PutMapping("/{documentId}/verify")
  @PreAuthorize("hasRole('ADMIN')")
  ApiResponse<FarmerDocumentResponse> verify(
      Authentication authentication,
      @PathVariable UUID documentId,
      @Valid @RequestBody FarmerDocumentVerificationRequest request
  ) {
    return ApiResponse.success(documentService.verifyForAdmin(
        CurrentUser.from(authentication),
        documentId,
        request
    ));
  }
}
