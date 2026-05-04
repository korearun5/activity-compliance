package com.activityplatform.backend.auth.service;

import com.activityplatform.backend.auth.config.SeedProperties;
import com.activityplatform.backend.auth.domain.RoleEntity;
import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.RoleRepository;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.security.Role;
import java.time.Instant;
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

  private static final String DEFAULT_TENANT_CODE = "default";

  private final PasswordEncoder passwordEncoder;
  private final RoleRepository roleRepository;
  private final SeedProperties seedProperties;
  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;

  public SeedDataInitializer(
      PasswordEncoder passwordEncoder,
      RoleRepository roleRepository,
      SeedProperties seedProperties,
      TenantRepository tenantRepository,
      UserRepository userRepository
  ) {
    this.passwordEncoder = passwordEncoder;
    this.roleRepository = roleRepository;
    this.seedProperties = seedProperties;
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!seedProperties.isEnabled()) {
      return;
    }

    Instant now = Instant.now();
    TenantEntity tenant = tenantRepository.findByCodeIgnoreCase(DEFAULT_TENANT_CODE)
        .orElseGet(() -> tenantRepository.save(new TenantEntity(
            UUID.randomUUID(),
            DEFAULT_TENANT_CODE,
            "Default Client",
            "ACTIVE",
            now
        )));

    RoleEntity adminRole = upsertRole(tenant, Role.ADMIN, now);
    RoleEntity participantRole = upsertRole(tenant, Role.PARTICIPANT, now);
    upsertRole(tenant, Role.SUPERVISOR, now);

    upsertUser(
        tenant,
        "admin",
        "admin123",
        "Platform Admin",
        "+91 00000 00000",
        "Head Office",
        "Admin",
        adminRole,
        now
    );
    upsertUser(
        tenant,
        "user",
        "user123",
        "Ravi Kumar",
        "+91 98765 43210",
        "North Block",
        "Rampur",
        participantRole,
        now
    );

    log.info("Seed data ensured for tenant={}", DEFAULT_TENANT_CODE);
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

  private void upsertUser(
      TenantEntity tenant,
      String username,
      String rawPassword,
      String displayName,
      String phone,
      String locationName,
      String siteName,
      RoleEntity role,
      Instant now
  ) {
    userRepository.findByTenantCodeIgnoreCaseAndUsernameIgnoreCase(tenant.getCode(), username)
        .ifPresentOrElse(
            user -> {
              if (user.getRoles().stream().noneMatch(existing -> existing.getCode().equals(role.getCode()))) {
                user.addRole(role);
                userRepository.save(user);
              }
            },
            () -> {
              UserEntity user = new UserEntity(
                  UUID.randomUUID(),
                  tenant,
                  username,
                  passwordEncoder.encode(rawPassword),
                  displayName,
                  phone,
                  locationName,
                  siteName,
                  "ACTIVE",
                  now
              );
              user.addRole(role);
              userRepository.save(user);
            }
        );
  }
}

