package com.activityplatform.backend.platform.api;

import com.activityplatform.backend.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform")
public class PlatformController {
  @GetMapping("/modules")
  ApiResponse<List<PlatformModuleDescriptor>> modules() {
    return ApiResponse.success(List.of(
        new PlatformModuleDescriptor("auth", "Authentication and JWT session management"),
        new PlatformModuleDescriptor("user", "Users, role management, and tenant membership"),
        new PlatformModuleDescriptor("workflow", "Configurable workflow and task definitions"),
        new PlatformModuleDescriptor("activity", "Workflow execution and task timeline tracking"),
        new PlatformModuleDescriptor("evidence", "Photo/file proof metadata and review status"),
        new PlatformModuleDescriptor("audit", "Append-only audit trail for compliance evidence"),
        new PlatformModuleDescriptor("reporting", "PDF, Excel, and analytics export foundation"),
        new PlatformModuleDescriptor("notification", "Notification template and delivery framework")
    ));
  }
}

