package com.activityplatform.backend.common.api;

public record ApiResponse<T>(
    boolean success,
    T data,
    ApiError error,
    ApiMeta meta
) {
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, data, null, null);
  }

  public static <T> ApiResponse<T> success(T data, ApiMeta meta) {
    return new ApiResponse<>(true, data, null, meta);
  }

  public static ApiResponse<Void> failure(ApiError error) {
    return new ApiResponse<>(false, null, error, null);
  }
}

