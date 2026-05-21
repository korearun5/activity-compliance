package com.activityplatform.backend.carbon.service;

import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;

record CarbonReportFilters(
    String village,
    String crop,
    String activityStatus,
    String verificationStatus,
    LocalDate dateFrom,
    LocalDate dateTo
) {
  static CarbonReportFilters empty() {
    return new CarbonReportFilters(null, null, null, null, null, null);
  }

  static CarbonReportFilters from(Map<String, Object> values) {
    if (values == null || values.isEmpty()) {
      return empty();
    }

    return new CarbonReportFilters(
        text(values, "village", "profileVillage"),
        text(values, "crop", "cropName", "primaryCrop"),
        text(values, "activityStatus"),
        text(values, "verificationStatus", "status"),
        date(values, "dateFrom", "from", "startDate"),
        date(values, "dateTo", "to", "endDate")
    );
  }

  boolean hasVerificationStatus() {
    return hasText(verificationStatus);
  }

  boolean matchesVillage(String value) {
    return matchesAny(village, Arrays.asList(value));
  }

  boolean matchesCrop(List<String> candidates) {
    return matchesAny(crop, candidates);
  }

  boolean matchesActivityStatus(String value) {
    return matchesAny(activityStatus, Arrays.asList(value));
  }

  boolean matchesVerificationStatus(String value) {
    return matchesAny(verificationStatus, Arrays.asList(value));
  }

  boolean matchesDate(LocalDate localDate, Instant... instants) {
    if (dateFrom == null && dateTo == null) {
      return true;
    }

    if (localDate != null && localDateInRange(localDate)) {
      return true;
    }

    for (Instant instant : instants) {
      if (instant != null && localDateInRange(LocalDate.ofInstant(instant, ZoneOffset.UTC))) {
        return true;
      }
    }

    return false;
  }

  private boolean localDateInRange(LocalDate value) {
    return (dateFrom == null || !value.isBefore(dateFrom))
        && (dateTo == null || !value.isAfter(dateTo));
  }

  private boolean matchesAny(String filter, List<String> candidates) {
    if (!hasText(filter)) {
      return true;
    }

    String normalizedFilter = normalize(filter);
    return candidates.stream()
        .filter(Objects::nonNull)
        .map(this::normalize)
        .anyMatch(candidate -> candidate.equals(normalizedFilter)
            || candidate.contains(normalizedFilter)
            || normalizedFilter.contains(candidate));
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase();
  }

  private static String text(Map<String, Object> values, String... keys) {
    for (String key : keys) {
      Object value = values.get(key);
      if (value != null && !value.toString().isBlank()) {
        return value.toString().trim();
      }
    }
    return null;
  }

  private static LocalDate date(Map<String, Object> values, String... keys) {
    String value = text(values, keys);
    if (value == null) {
      return null;
    }

    try {
      return LocalDate.parse(value);
    } catch (RuntimeException exception) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_FAILED,
          "Carbon report date filters must use YYYY-MM-DD format.",
          HttpStatus.BAD_REQUEST
      );
    }
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
