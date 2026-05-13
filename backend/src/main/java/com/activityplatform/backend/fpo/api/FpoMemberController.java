package com.activityplatform.backend.fpo.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.common.api.PageResponse;
import com.activityplatform.backend.fpo.domain.FpoMemberStatus;
import com.activityplatform.backend.fpo.service.FpoMemberService;
import com.activityplatform.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/v1/fpo/members")
public class FpoMemberController {
  private final FpoMemberService fpoMemberService;

  public FpoMemberController(FpoMemberService fpoMemberService) {
    this.fpoMemberService = fpoMemberService;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
  ApiResponse<PageResponse<FpoMemberResponse>> list(
      Authentication authentication,
      @RequestParam(required = false) FpoMemberStatus status,
      @PageableDefault(size = 20, page = 0) Pageable pageable
  ) {
    return ApiResponse.success(PageResponse.from(
        fpoMemberService.list(CurrentUser.from(authentication), status, pageable)));
  }

  @GetMapping("/me")
  ApiResponse<FpoMemberResponse> me(Authentication authentication) {
    return ApiResponse.success(fpoMemberService.me(CurrentUser.from(authentication)));
  }

  @GetMapping("/{memberId}")
  ApiResponse<FpoMemberResponse> get(
      Authentication authentication,
      @PathVariable UUID memberId
  ) {
    return ApiResponse.success(fpoMemberService.get(CurrentUser.from(authentication), memberId));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
  ApiResponse<FpoMemberResponse> create(
      Authentication authentication,
      @Valid @RequestBody CreateFpoMemberRequest request
  ) {
    return ApiResponse.success(fpoMemberService.create(CurrentUser.from(authentication), request));
  }

  @PutMapping("/{memberId}")
  @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
  ApiResponse<FpoMemberResponse> update(
      Authentication authentication,
      @PathVariable UUID memberId,
      @Valid @RequestBody UpdateFpoMemberRequest request
  ) {
    return ApiResponse.success(
        fpoMemberService.update(CurrentUser.from(authentication), memberId, request));
  }

  @PatchMapping("/{memberId}/status")
  @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
  ApiResponse<FpoMemberResponse> updateStatus(
      Authentication authentication,
      @PathVariable UUID memberId,
      @Valid @RequestBody UpdateFpoMemberStatusRequest request
  ) {
    return ApiResponse.success(fpoMemberService.updateStatus(
        CurrentUser.from(authentication),
        memberId,
        request.status()
    ));
  }
}
