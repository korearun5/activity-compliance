package com.activityplatform.backend.evidence.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.evidence.service.EvidenceService;
import com.activityplatform.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/evidence")
public class EvidenceController {
  private final EvidenceService evidenceService;

  public EvidenceController(EvidenceService evidenceService) {
    this.evidenceService = evidenceService;
  }

  @GetMapping
  ApiResponse<List<EvidenceResponse>> list(
      Authentication authentication,
      @RequestParam(required = false) UUID activityId
  ) {
    return ApiResponse.success(evidenceService.list(CurrentUser.from(authentication), activityId));
  }

  @GetMapping("/{evidenceId}")
  ApiResponse<EvidenceResponse> get(
      Authentication authentication,
      @PathVariable UUID evidenceId
  ) {
    return ApiResponse.success(evidenceService.get(CurrentUser.from(authentication), evidenceId));
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ApiResponse<EvidenceResponse> upload(
      Authentication authentication,
      @RequestParam UUID activityId,
      @RequestParam UUID activityTaskId,
      @RequestPart MultipartFile file,
      @RequestParam(required = false) String note
  ) {
    return ApiResponse.success(evidenceService.upload(
        CurrentUser.from(authentication),
        activityId,
        activityTaskId,
        file,
        note
    ));
  }

  @PatchMapping("/{evidenceId}/review")
  @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
  ApiResponse<EvidenceResponse> review(
      Authentication authentication,
      @PathVariable UUID evidenceId,
      @Valid @RequestBody EvidenceReviewRequest request
  ) {
    return ApiResponse.success(evidenceService.review(
        CurrentUser.from(authentication),
        evidenceId,
        request.status()
    ));
  }
}
