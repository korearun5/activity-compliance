package com.activityplatform.backend.fpo.service;

import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;

final class FarmAssetRules {
  static final String OWNERSHIP_SELF_OWNED = "Self-owned";
  static final String OWNERSHIP_LEASED_IN = "Leased-in";
  static final String OWNERSHIP_SHARECROPPER = "Sharecropper";
  static final String IRRIGATION_CANAL = "Canal";
  static final String IRRIGATION_BOREWELL = "Borewell";
  static final String IRRIGATION_OPEN_WELL = "Open well";
  static final String IRRIGATION_POND = "Pond";
  static final String IRRIGATION_RAINFED = "Rainfed";
  static final String IRRIGATION_DRIP = "Drip";

  private static final Map<String, String> OWNERSHIP_VALUES = Map.of(
      normalizedKey(OWNERSHIP_SELF_OWNED), OWNERSHIP_SELF_OWNED,
      normalizedKey(OWNERSHIP_LEASED_IN), OWNERSHIP_LEASED_IN,
      normalizedKey(OWNERSHIP_SHARECROPPER), OWNERSHIP_SHARECROPPER
  );

  private static final Map<String, String> IRRIGATION_VALUES = Map.of(
      normalizedKey(IRRIGATION_CANAL), IRRIGATION_CANAL,
      normalizedKey(IRRIGATION_BOREWELL), IRRIGATION_BOREWELL,
      normalizedKey(IRRIGATION_OPEN_WELL), IRRIGATION_OPEN_WELL,
      normalizedKey(IRRIGATION_POND), IRRIGATION_POND,
      normalizedKey(IRRIGATION_RAINFED), IRRIGATION_RAINFED,
      normalizedKey(IRRIGATION_DRIP), IRRIGATION_DRIP
  );

  private FarmAssetRules() {
  }

  static String normalizeRequiredText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw validation(fieldName + " is required.");
    }
    return value.trim();
  }

  static String normalizeOwnershipType(String value) {
    return normalizeApprovedValue(
        value,
        "Ownership type",
        OWNERSHIP_VALUES,
        "Self-owned, Leased-in, Sharecropper"
    );
  }

  static String normalizeIrrigationSource(String value) {
    return normalizeApprovedValue(
        value,
        "Irrigation source",
        IRRIGATION_VALUES,
        "Canal, Borewell, Open well, Pond, Rainfed, Drip"
    );
  }

  static void validateGpsPoint(BigDecimal latitude, BigDecimal longitude) {
    if (latitude == null) {
      throw validation("GPS latitude is required.");
    }

    if (longitude == null) {
      throw validation("GPS longitude is required.");
    }

    if (latitude.compareTo(new BigDecimal("-90")) < 0
        || latitude.compareTo(new BigDecimal("90")) > 0) {
      throw validation("Latitude must be between -90 and 90.");
    }

    if (longitude.compareTo(new BigDecimal("-180")) < 0
        || longitude.compareTo(new BigDecimal("180")) > 0) {
      throw validation("Longitude must be between -180 and 180.");
    }
  }

  private static String normalizeApprovedValue(
      String value,
      String fieldName,
      Map<String, String> approvedValues,
      String displayValues
  ) {
    String normalized = normalizeRequiredText(value, fieldName);
    String approved = approvedValues.get(normalizedKey(normalized));
    if (approved == null) {
      throw validation(fieldName + " must be one of: " + displayValues + ".");
    }
    return approved;
  }

  private static String normalizedKey(String value) {
    return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
  }

  private static ApplicationException validation(String message) {
    return new ApplicationException(ErrorCode.VALIDATION_FAILED, message, HttpStatus.BAD_REQUEST);
  }
}
