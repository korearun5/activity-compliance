package com.activityplatform.backend.fpo.service;

import com.activityplatform.backend.farmer.FarmerProfileRules;

final class FpoMemberProfileRules {
  private FpoMemberProfileRules() {
  }

  static String normalizeIndianMobile(String value) {
    return FarmerProfileRules.normalizeIndianMobile(value);
  }

  static String normalizeOptionalIndianMobile(String value) {
    return FarmerProfileRules.normalizeOptionalIndianMobile(value);
  }

  static String normalizeOptionalAadhaar(String value) {
    return FarmerProfileRules.normalizeOptionalAadhaar(value);
  }

  static String normalizeGender(String value) {
    return FarmerProfileRules.normalizeGender(value);
  }

  static String normalizeFarmerCategory(String value) {
    return FarmerProfileRules.normalizeFarmerCategory(value);
  }
}
