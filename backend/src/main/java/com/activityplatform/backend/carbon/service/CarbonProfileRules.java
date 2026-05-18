package com.activityplatform.backend.carbon.service;

import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.farmer.FarmerProfileRules;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.springframework.http.HttpStatus;

final class CarbonProfileRules {
  private static final List<String> TILLAGE_STATUSES = List.of(
      "Conventional",
      "Reduced tillage",
      "No tillage"
  );
  private static final List<String> BANK_STATUSES = List.of("Linked", "Pending", "Not required");
  private static final List<String> AADHAAR_STATUSES = List.of(
      "Provided",
      "Optional not captured"
  );
  private static final List<String> DOCUMENT_STATUSES = List.of("Not started", "Partial", "Ready");

  private CarbonProfileRules() {
  }

  static String normalizeRequiredText(String value, String label) {
    if (!hasText(value)) {
      throw validation(label + " is required.");
    }
    return value.trim();
  }

  static String normalizeOptionalText(String value) {
    return hasText(value) ? value.trim() : null;
  }

  static String normalizeOptionalIndianMobile(String value) {
    return FarmerProfileRules.normalizeOptionalIndianMobile(value);
  }

  static String normalizeRequiredIndianMobile(String value) {
    return FarmerProfileRules.normalizeIndianMobile(value);
  }

  static String normalizeOptionalAadhaarNumber(String value) {
    return FarmerProfileRules.normalizeOptionalAadhaar(value);
  }

  static String normalizeGender(String value) {
    return FarmerProfileRules.normalizeGender(value);
  }

  static String normalizeFarmerCategory(String value) {
    return FarmerProfileRules.normalizeFarmerCategory(value);
  }

  static String normalizeOptionalUsername(String value) {
    return normalizeOptionalText(value);
  }

  static String normalizeRequiredUsername(String value) {
    return normalizeRequiredText(value, "Username");
  }

  static String normalizeTillageStatus(String value) {
    return normalizeOptionalChoice(value, TILLAGE_STATUSES, "Tillage status");
  }

  static String normalizeBankStatus(String value) {
    return normalizeOptionalChoice(value, BANK_STATUSES, "Bank status");
  }

  static String normalizeAadhaarStatus(String value) {
    return normalizeOptionalChoice(value, AADHAAR_STATUSES, "Aadhaar status");
  }

  static String normalizeDocumentStatus(String value) {
    return normalizeOptionalChoice(value, DOCUMENT_STATUSES, "Document status");
  }

  static String normalizeReportContentType(String value) {
    String normalized = normalizeOptionalText(value);
    if (normalized == null) {
      return null;
    }

    if (!normalized.equals("application/pdf") && !normalized.startsWith("image/")) {
      throw validation("Report content type must be application/pdf or an image content type.");
    }

    return normalized;
  }

  static String normalizeReportUrl(String value) {
    String normalized = normalizeOptionalText(value);
    if (normalized == null) {
      return null;
    }

    try {
      URI uri = new URI(normalized);
      String scheme = uri.getScheme();
      if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
        throw validation("Report URL must start with http or https.");
      }
    } catch (URISyntaxException exception) {
      throw validation("Report URL must be valid.");
    }

    return normalized;
  }

  static void validateOptionalGpsPair(BigDecimal latitude, BigDecimal longitude) {
    if ((latitude == null) != (longitude == null)) {
      throw validation("Both latitude and longitude are required when capturing a GPS point.");
    }
  }

  private static String normalizeOptionalChoice(
      String value,
      List<String> allowedValues,
      String label
  ) {
    String normalized = normalizeOptionalText(value);
    if (normalized == null) {
      return null;
    }

    return allowedValues.stream()
        .filter(allowed -> allowed.equalsIgnoreCase(normalized))
        .findFirst()
        .orElseThrow(() -> validation(label + " is not supported."));
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static ApplicationException validation(String message) {
    return new ApplicationException(ErrorCode.VALIDATION_FAILED, message, HttpStatus.BAD_REQUEST);
  }
}
