package com.activityplatform.backend.workflow.api;

import com.activityplatform.backend.common.api.ApiResponse;
import com.activityplatform.backend.common.api.PageResponse;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.workflow.domain.WorkflowDefinitionStatus;
import com.activityplatform.backend.workflow.service.WorkflowDefinitionService;
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
@RequestMapping("/api/v1/workflows")
public class WorkflowController {
  private final WorkflowDefinitionService workflowDefinitionService;

  public WorkflowController(WorkflowDefinitionService workflowDefinitionService) {
    this.workflowDefinitionService = workflowDefinitionService;
  }

  @GetMapping
  ApiResponse<PageResponse<WorkflowResponse>> list(
      Authentication authentication,
      @RequestParam(required = false) WorkflowDefinitionStatus status,
      @PageableDefault(size = 20, page = 0) Pageable pageable) {
    return ApiResponse.success(
        PageResponse.from(workflowDefinitionService.list(CurrentUser.from(authentication), status, pageable)));
  }

  @GetMapping("/{workflowId}")
  ApiResponse<WorkflowResponse> get(
      Authentication authentication,
      @PathVariable UUID workflowId) {
    return ApiResponse.success(workflowDefinitionService.get(CurrentUser.from(authentication), workflowId));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER')")
  ApiResponse<WorkflowResponse> create(
      Authentication authentication,
      @Valid @RequestBody WorkflowRequest request) {
    return ApiResponse.success(
        workflowDefinitionService.create(CurrentUser.from(authentication), request));
  }

  @PutMapping("/{workflowId}")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER')")
  ApiResponse<WorkflowResponse> update(
      Authentication authentication,
      @PathVariable UUID workflowId,
      @Valid @RequestBody WorkflowRequest request) {
    return ApiResponse.success(
        workflowDefinitionService.update(CurrentUser.from(authentication), workflowId, request));
  }

  @PatchMapping("/{workflowId}/status")
  @PreAuthorize("hasAnyRole('ADMIN','FPO_MANAGER')")
  ApiResponse<WorkflowResponse> updateStatus(
      Authentication authentication,
      @PathVariable UUID workflowId,
      @Valid @RequestBody WorkflowStatusRequest request) {
    return ApiResponse.success(
        workflowDefinitionService.updateStatus(
            CurrentUser.from(authentication),
            workflowId,
            request.status()));
  }
}
