package com.activityplatform.backend.farmer.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.farmer.domain.FarmerDocumentType;
import com.activityplatform.backend.farmer.service.FarmerBankDetailsService;
import com.activityplatform.backend.farmer.service.FarmerDocumentService;
import com.activityplatform.backend.farmer.service.FarmerProfileCompletionService;
import com.activityplatform.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/farmer")
public class FarmerProfileController {
  private final FarmerBankDetailsService bankDetailsService;
  private final FarmerProfileCompletionService completionService;
  private final FarmerDocumentService documentService;

  public FarmerProfileController(
      FarmerBankDetailsService bankDetailsService,
      FarmerProfileCompletionService completionService,
      FarmerDocumentService documentService
  ) {
    this.bankDetailsService = bankDetailsService;
    this.completionService = completionService;
    this.documentService = documentService;
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

  @GetMapping("/documents")
  @PreAuthorize("hasRole('FARMER')")
  ApiResponse<List<FarmerDocumentResponse>> listDocuments(Authentication authentication) {
    return ApiResponse.success(
        documentService.listCurrentFarmerDocuments(CurrentUser.from(authentication))
    );
  }

  @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('FARMER')")
  ApiResponse<FarmerDocumentResponse> uploadDocument(
      Authentication authentication,
      @RequestParam("document_type") FarmerDocumentType documentType,
      @RequestPart MultipartFile file
  ) {
    return ApiResponse.success(
        documentService.uploadCurrentFarmerDocument(
            CurrentUser.from(authentication),
            documentType,
            file
        )
    );
  }

  @DeleteMapping("/documents/{documentId}")
  @PreAuthorize("hasRole('FARMER')")
  ApiResponse<Void> deleteDocument(
      Authentication authentication,
      @PathVariable UUID documentId
  ) {
    documentService.deleteCurrentFarmerDocument(CurrentUser.from(authentication), documentId);
    return ApiResponse.success(null);
  }

  @GetMapping("/profile/completion")
  @PreAuthorize("hasRole('FARMER')")
  ApiResponse<FarmerProfileCompletionResponse> completion(Authentication authentication) {
    return ApiResponse.success(
        completionService.getCurrentFarmerCompletion(CurrentUser.from(authentication))
    );
  }
}
