package com.activityplatform.backend.platform.domain;

import java.util.Arrays;

public enum ModuleCode {
  MEMBER_DATA,
  LAND_RECORDS,
  GEO_TAGGING,
  CROP_PLANNING,
  INPUT_DEMAND,
  ADVISORY,
  ACTIVITY_COMPLIANCE,
  EVIDENCE_REVIEW,
  REPORT_EXPORT,
  INVENTORY,
  PROCUREMENT,
  TRACEABILITY,
  SUSTAINABILITY,
  ANALYTICS;

  public static ModuleCode from(String value) {
    return Arrays.stream(values())
        .filter(code -> code.name().equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported module code: " + value));
  }
}
