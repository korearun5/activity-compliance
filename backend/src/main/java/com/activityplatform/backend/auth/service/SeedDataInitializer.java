package com.activityplatform.backend.auth.service;

import com.activityplatform.backend.auth.config.SeedProperties;
import com.activityplatform.backend.auth.config.SeedProperties.SeedUser;
import com.activityplatform.backend.auth.domain.RoleEntity;
import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.RoleRepository;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.platform.service.TenantModuleService;
import com.activityplatform.backend.security.Role;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeedDataInitializer implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(SeedDataInitializer.class);

  private final PasswordEncoder passwordEncoder;
  private final RoleRepository roleRepository;
  private final SeedProperties seedProperties;
  private final TenantRepository tenantRepository;
  private final TenantModuleService tenantModuleService;
  private final UserRepository userRepository;

  public SeedDataInitializer(
      PasswordEncoder passwordEncoder,
      RoleRepository roleRepository,
      SeedProperties seedProperties,
      TenantRepository tenantRepository,
      TenantModuleService tenantModuleService,
      UserRepository userRepository
  ) {
    this.passwordEncoder = passwordEncoder;
    this.roleRepository = roleRepository;
    this.seedProperties = seedProperties;
    this.tenantRepository = tenantRepository;
    this.tenantModuleService = tenantModuleService;
    this.userRepository = userRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!seedProperties.isEnabled()) {
      return;
    }

    Instant now = Instant.now();
    TenantEntity tenant = tenantRepository.findByCodeIgnoreCase(seedProperties.getTenantCode())
        .orElseGet(() -> tenantRepository.save(new TenantEntity(
            UUID.randomUUID(),
            seedProperties.getTenantCode(),
            seedProperties.getTenantName(),
            "ACTIVE",
            now
        )));

    for (Role role : Role.values()) {
      upsertRole(tenant, role, now);
    }

    List<SeedUser> validSeedUsers = seedProperties.getUsers().stream()
        .filter(this::isValidSeedUser)
        .toList();

    validSeedUsers.forEach(user -> upsertUser(tenant, user, now));
    tenantModuleService.enableSeedModules(tenant, seedProperties.getEnabledModules());

    log.info(
        "Seed data ensured for tenant={} userCount={}",
        seedProperties.getTenantCode(),
        validSeedUsers.size()
    );
  }

  private RoleEntity upsertRole(TenantEntity tenant, Role role, Instant now) {
    return roleRepository.findByTenantCodeIgnoreCaseAndCodeIgnoreCase(tenant.getCode(), role.name())
        .orElseGet(() -> roleRepository.save(new RoleEntity(
            UUID.randomUUID(),
            tenant,
            role.name(),
            role.name(),
            now
        )));
  }

  private void upsertUser(TenantEntity tenant, SeedUser seedUser, Instant now) {
    List<RoleEntity> roles = seedUser.getRoles().stream()
        .filter(Objects::nonNull)
        .filter(this::isValidRole)
        .map(role -> Role.valueOf(role.toUpperCase()))
        .map(role -> upsertRole(tenant, role, now))
        .toList();

    userRepository.findByTenantCodeIgnoreCaseAndUsernameIgnoreCase(
            tenant.getCode(),
            seedUser.getUsername()
        )
        .ifPresentOrElse(
            user -> {
              for (RoleEntity role : roles) {
                user.addRole(role);
              }
              userRepository.save(user);
            },
            () -> {
              UserEntity user = new UserEntity(
                  UUID.randomUUID(),
                  tenant,
                  seedUser.getUsername(),
                  passwordEncoder.encode(seedUser.getPassword()),
                  seedUser.getDisplayName(),
                  seedUser.getPhone(),
                  seedUser.getLocationName(),
                  seedUser.getSiteName(),
                  "ACTIVE",
                  now
              );
              roles.forEach(user::addRole);
              userRepository.save(user);
            }
        );
  }

  private boolean isValidSeedUser(SeedUser seedUser) {
    return seedUser != null
        && hasText(seedUser.getUsername())
        && hasText(seedUser.getPassword())
        && hasText(seedUser.getDisplayName())
        && seedUser.getRoles() != null
        && seedUser.getRoles().stream().filter(Objects::nonNull).anyMatch(this::isValidRole);
  }

  private boolean isValidRole(String role) {
    try {
      Role.valueOf(role.toUpperCase());
      return true;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
