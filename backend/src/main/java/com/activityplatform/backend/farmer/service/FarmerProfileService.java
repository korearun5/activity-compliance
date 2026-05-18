package com.activityplatform.backend.farmer.service;

import com.activityplatform.backend.auth.domain.RoleEntity;
import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.farmer.FarmerProfileRules;
import com.activityplatform.backend.farmer.domain.FarmerProfileEntity;
import com.activityplatform.backend.farmer.domain.FarmerProfileStatus;
import com.activityplatform.backend.farmer.repository.FarmerProfileRepository;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FarmerProfileService implements FarmerService {
  private final FarmerProfileRepository farmerProfileRepository;
  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;

  public FarmerProfileService(
      FarmerProfileRepository farmerProfileRepository,
      TenantRepository tenantRepository,
      UserRepository userRepository
  ) {
    this.farmerProfileRepository = farmerProfileRepository;
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
  }

  @Override
  @Transactional
  public FarmerProfileEntity createFarmerProfile(
      CurrentUser currentUser,
      UserEntity farmerUser,
      FarmerProfileCommand command
  ) {
    TenantEntity tenant = requireTenant(currentUser.tenantId());
    UserEntity linkedUser = requireFarmerUser(currentUser, farmerUser);
    if (farmerProfileRepository.existsByTenantIdAndUserId(tenant.getId(), linkedUser.getId())) {
      throw conflict("A farmer profile already exists for this user.");
    }

    NormalizedFarmerProfileCommand normalized = normalize(command);
    Instant now = Instant.now();
    FarmerProfileEntity profile = new FarmerProfileEntity(
        UUID.randomUUID(),
        tenant,
        linkedUser,
        normalized.displayName(),
        normalized.mobileNumber(),
        normalized.alternateMobileNumber(),
        normalized.aadhaarNumber(),
        normalized.village(),
        normalized.taluka(),
        normalized.districtName(),
        normalized.stateName(),
        normalized.gender(),
        normalized.dateOfBirth(),
        normalized.age(),
        normalized.farmerCategory(),
        normalized.status(),
        actor(currentUser),
        now
    );
    return save(profile);
  }

  @Override
  @Transactional
  public FarmerProfileEntity ensureFarmerProfileForUser(
      CurrentUser currentUser,
      UserEntity farmerUser,
      FarmerProfileCommand command
  ) {
    UserEntity linkedUser = requireFarmerUser(currentUser, farmerUser);
    return farmerProfileRepository.findByTenantIdAndUserId(currentUser.tenantId(), linkedUser.getId())
        .orElseGet(() -> createFarmerProfile(currentUser, linkedUser, command));
  }

  @Override
  @Transactional
  public FarmerProfileEntity updateFarmerProfile(
      CurrentUser currentUser,
      UUID farmerProfileId,
      FarmerProfileCommand command
  ) {
    FarmerProfileEntity profile = requireById(currentUser.tenantId(), farmerProfileId);
    NormalizedFarmerProfileCommand normalized = normalize(command);
    profile.updateDetails(
        normalized.displayName(),
        normalized.mobileNumber(),
        normalized.alternateMobileNumber(),
        normalized.aadhaarNumber(),
        normalized.village(),
        normalized.taluka(),
        normalized.districtName(),
        normalized.stateName(),
        normalized.gender(),
        normalized.dateOfBirth(),
        normalized.age(),
        normalized.farmerCategory(),
        normalized.status(),
        Instant.now()
    );
    return save(profile);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<FarmerProfileEntity> findByUserId(UUID tenantId, UUID userId) {
    return farmerProfileRepository.findByTenantIdAndUserId(tenantId, userId);
  }

  @Override
  @Transactional(readOnly = true)
  public FarmerProfileEntity requireById(UUID tenantId, UUID farmerProfileId) {
    return farmerProfileRepository.findByIdAndTenantId(farmerProfileId, tenantId)
        .orElseThrow(() -> notFound("Farmer profile not found."));
  }

  @Override
  @Transactional(readOnly = true)
  public FarmerProfileEntity requireByUserId(UUID tenantId, UUID userId) {
    return farmerProfileRepository.findByTenantIdAndUserId(tenantId, userId)
        .orElseThrow(() -> notFound("Farmer profile not found for user."));
  }

  @Override
  @Transactional(readOnly = true)
  public List<FarmerParticipant> findParticipants(UUID tenantId) {
    return farmerProfileRepository
        .findByTenantIdAndStatusOrderByDisplayNameAsc(tenantId, FarmerProfileStatus.ACTIVE)
        .stream()
        .map(profile -> new FarmerParticipant(
            profile.getId(),
            profile.getUser().getId(),
            profile.getUser().getUsername(),
            profile.getDisplayName(),
            profile.getMobileNumber(),
            profile.getVillage(),
            profile.getTaluka(),
            profile.getDistrictName(),
            profile.getStateName(),
            profile.getStatus()
        ))
        .toList();
  }

  private UserEntity requireFarmerUser(CurrentUser currentUser, UserEntity farmerUser) {
    if (farmerUser == null || farmerUser.getId() == null) {
      throw validation("Farmer user is required.");
    }

    UserEntity user = userRepository.findByIdAndTenantId(farmerUser.getId(), currentUser.tenantId())
        .orElseThrow(() -> notFound("Farmer user not found."));
    requireFarmerOnly(user);
    return user;
  }

  private void requireFarmerOnly(UserEntity user) {
    Set<Role> roles = user.getRoles().stream()
        .map(RoleEntity::getCode)
        .map(Role::valueOf)
        .collect(Collectors.toUnmodifiableSet());

    if (!roles.equals(Set.of(Role.FARMER))) {
      throw validation("Farmer profiles must be linked to farmer-only users.");
    }
  }

  private TenantEntity requireTenant(UUID tenantId) {
    return tenantRepository.findById(tenantId)
        .orElseThrow(() -> notFound("Tenant not found."));
  }

  private NormalizedFarmerProfileCommand normalize(FarmerProfileCommand command) {
    if (command == null) {
      throw validation("Farmer profile details are required.");
    }

    LocalDate dateOfBirth = command.dateOfBirth();
    if (dateOfBirth != null && dateOfBirth.isAfter(LocalDate.now())) {
      throw validation("Date of birth cannot be in the future.");
    }

    return new NormalizedFarmerProfileCommand(
        normalizeRequired(command.displayName(), "Full name"),
        FarmerProfileRules.normalizeIndianMobile(command.mobileNumber()),
        FarmerProfileRules.normalizeOptionalIndianMobile(command.alternateMobileNumber()),
        FarmerProfileRules.normalizeOptionalAadhaar(command.aadhaarNumber()),
        normalizeRequired(command.village(), "Village"),
        normalizeRequired(command.taluka(), "Taluka"),
        normalizeRequired(command.districtName(), "District"),
        normalizeRequired(command.stateName(), "State"),
        FarmerProfileRules.normalizeGender(command.gender()),
        dateOfBirth,
        normalizeAge(command.age()),
        FarmerProfileRules.normalizeFarmerCategory(command.farmerCategory()),
        command.status() == null ? FarmerProfileStatus.ACTIVE : command.status()
    );
  }

  private Integer normalizeAge(Integer age) {
    if (age == null) {
      return null;
    }

    if (age < 0 || age > 120) {
      throw validation("Age must be between 0 and 120 when provided.");
    }

    return age;
  }

  private String normalizeRequired(String value, String label) {
    if (value == null || value.isBlank()) {
      throw validation(label + " is required.");
    }
    return value.trim();
  }

  private FarmerProfileEntity save(FarmerProfileEntity profile) {
    try {
      return farmerProfileRepository.saveAndFlush(profile);
    } catch (DataIntegrityViolationException exception) {
      throw conflict("Farmer profile user and Aadhaar must be unique per tenant.");
    }
  }

  private UserEntity actor(CurrentUser currentUser) {
    return userRepository.findById(currentUser.userId()).orElse(null);
  }

  private ApplicationException validation(String message) {
    return new ApplicationException(ErrorCode.VALIDATION_FAILED, message, HttpStatus.BAD_REQUEST);
  }

  private ApplicationException conflict(String message) {
    return new ApplicationException(ErrorCode.DUPLICATE_RESOURCE, message, HttpStatus.CONFLICT);
  }

  private ApplicationException notFound(String message) {
    return new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, message, HttpStatus.NOT_FOUND);
  }

  private record NormalizedFarmerProfileCommand(
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
}
