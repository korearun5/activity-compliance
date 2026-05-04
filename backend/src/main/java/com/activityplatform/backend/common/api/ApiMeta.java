package com.activityplatform.backend.common.api;

public record ApiMeta(
    PageMeta page,
    String requestId
) {
  public static ApiMeta request(String requestId) {
    return new ApiMeta(null, requestId);
  }
}

