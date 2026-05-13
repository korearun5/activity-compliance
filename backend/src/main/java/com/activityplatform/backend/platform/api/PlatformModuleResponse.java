package com.activityplatform.backend.platform.api;

import com.activityplatform.backend.platform.domain.PlatformModuleEntity;

public record PlatformModuleResponse(
    String code,
    String name,
    String description,
    String status
) {
  public static PlatformModuleResponse from(PlatformModuleEntity module) {
    return new PlatformModuleResponse(
        module.getCode().name(),
        module.getName(),
        module.getDescription(),
        module.getStatus().name()
    );
  }
}
