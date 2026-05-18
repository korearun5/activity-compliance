package com.activityplatform.backend.farmer.service;

import com.activityplatform.backend.farmer.domain.FarmerProfileStatus;
import java.time.LocalDate;

public record FarmerProfileCommand(
    String displayName,
    String mobileNumber,
    String alternateMobileNumber,
    String aadhaarNumber,
    String village,
    String taluka,
    String districtName,
    String stateName,
    String gender,
    LocalDate dateOfBirth,
    Integer age,
    String farmerCategory,
    FarmerProfileStatus status
) {
}
