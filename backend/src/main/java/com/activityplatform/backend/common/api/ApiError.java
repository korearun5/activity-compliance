package com.activityplatform.backend.common.api;

import java.util.List;
import java.util.Map;

public record ApiError(
    String code,
    String message,
    Map<String, List<String>> details,
    String traceId
) {
}

