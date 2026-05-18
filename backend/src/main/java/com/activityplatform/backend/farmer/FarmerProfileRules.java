package com.activityplatform.backend.farmer;

import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;

public final class FarmerProfileRules {
  private static final Set<String> GENDERS = Set.of("MALE", "FEMALE", "OTHER");
  private static final Set<String> FARMER_CATEGORIES = Set.of(
      "MARGINAL",
      "SMALL",
      "SEMI_MEDIUM",
      "MEDIUM",
      "LARGE"
  );
  private static final Map<String, String> CATEGORY_ALIASES = Map.of(
      "SEMI-MEDIUM", "SEMI_MEDIUM",
      "SEMI MEDIUM", "SEMI_MEDIUM"
  );

  private FarmerProfileRules() {
  }

  public static String normalizeIndianMobile(String value) {
    String digits = value == null ? "" : value.replaceAll("\\D", "");
    if (digits.length() == 12 && digits.startsWith("91")) {
      digits = digits.substring(2);
    }

    if (!digits.matches("[6-9][0-9]{9}")) {
      throw validation("Mobile number must be a 10 digit Indian mobile number.");
    }

    return digits;
  }

  public static String normalizeOptionalIndianMobile(String value) {
    return hasText(value) ? normalizeIndianMobile(value) : null;
  }

  public static String normalizeOptionalAadhaar(String value) {
    if (!hasText(value)) {
      return null;
    }

    String digits = value.replaceAll("\\D", "");
    if (!digits.matches("[0-9]{12}")) {
      throw validation("Aadhaar number must be 12 digits when provided.");
    }

    return digits;
  }

  public static String normalizeGender(String value) {
    String normalized = normalizeRequiredCode(value, "Gender");
    if (!GENDERS.contains(normalized)) {
      throw validation("Gender must be MALE, FEMALE, or OTHER.");
    }
    return normalized;
  }

  public static String normalizeFarmerCategory(String value) {
    String normalized = normalizeRequiredCode(value, "Farmer category");
    normalized = CATEGORY_ALIASES.getOrDefault(normalized, normalized);
    if (!FARMER_CATEGORIES.contains(normalized)) {
      throw validation(
          "Farmer category must be MARGINAL, SMALL, SEMI_MEDIUM, MEDIUM, or LARGE."
      );
    }
    return normalized;
  }

  public static String normalizeRequiredCode(String value, String label) {
    if (!hasText(value)) {
      throw validation(label + " is required.");
    }
    return value.trim()
        .replace('-', '_')
        .replace(' ', '_')
        .toUpperCase(Locale.ROOT);
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static ApplicationException validation(String message) {
    return new ApplicationException(ErrorCode.VALIDATION_FAILED, message, HttpStatus.BAD_REQUEST);
  }
}
