package com.activityplatform.backend.platform.service;

import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.domain.AuditAction;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.common.error.ErrorCode;
import com.activityplatform.backend.platform.api.PlatformModuleResponse;
import com.activityplatform.backend.platform.api.TenantModuleSubscriptionResponse;
import com.activityplatform.backend.platform.domain.ModuleCode;
import com.activityplatform.backend.platform.domain.PlatformModuleEntity;
import com.activityplatform.backend.platform.domain.TenantModuleSubscriptionEntity;
import com.activityplatform.backend.platform.domain.TenantModuleSubscriptionStatus;
import com.activityplatform.backend.platform.repository.PlatformModuleRepository;
import com.activityplatform.backend.platform.repository.TenantModuleSubscriptionRepository;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantModuleService {
  private static final Set<ModuleCode> DEFAULT_FPO_MODULES = EnumSet.of(
      ModuleCode.MEMBER_DATA,
      ModuleCode.LAND_RECORDS,
      ModuleCode.GEO_TAGGING,
      ModuleCode.CROP_PLANNING,
      ModuleCode.INPUT_DEMAND,
      ModuleCode.ADVISORY,
      ModuleCode.ACTIVITY_COMPLIANCE,
      ModuleCode.EVIDENCE_REVIEW,
      ModuleCode.REPORT_EXPORT
  );

  private final AuditEventService auditEventService;
  private final PlatformModuleRepository platformModuleRepository;
  private final TenantModuleSubscriptionRepository subscriptionRepository;
  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;

  public TenantModuleService(
      AuditEventService auditEventService,
      PlatformModuleRepository platformModuleRepository,
      TenantModuleSubscriptionRepository subscriptionRepository,
      TenantRepository tenantRepository,
      UserRepository userRepository
  ) {
    this.auditEventService = auditEventService;
    this.platformModuleRepository = platformModuleRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<PlatformModuleResponse> listCatalog() {
    return platformModuleRepository.findAllByOrderByCodeAsc().stream()
        .map(PlatformModuleResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ModuleCode> findEnabledModuleCodes(UUID tenantId) {
    Instant now = Instant.now();
    return subscriptionRepository.findByTenantId(tenantId).stream()
        .filter(subscription -> subscription.isEnabledAt(now))
        .map(subscription -> subscription.getModule().getCode())
        .sorted(Comparator.comparing(Enum::name))
        .toList();
  }

  @Transactional(readOnly = true)
  public void requireEnabled(UUID tenantId, ModuleCode moduleCode) {
    boolean enabled = subscriptionRepository.findByTenantIdAndModuleCode(tenantId, moduleCode)
        .filter(subscription -> subscription.isEnabledAt(Instant.now()))
        .isPresent();

    if (!enabled) {
      throw new ApplicationException(
          ErrorCode.MODULE_NOT_ENABLED,
          "Module " + moduleCode.name() + " is not enabled for this tenant.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  @Transactional(readOnly = true)
  public List<TenantModuleSubscriptionResponse> listSubscriptions(CurrentUser currentUser) {
    requireAdmin(currentUser);
    Instant now = Instant.now();
    return subscriptionRepository.findByTenantIdOrderByModuleCode(currentUser.tenantId()).stream()
        .map(subscription -> TenantModuleSubscriptionResponse.from(subscription, now))
        .toList();
  }

  @Transactional
  public TenantModuleSubscriptionResponse updateSubscription(
      CurrentUser currentUser,
      ModuleCode moduleCode,
      TenantModuleSubscriptionStatus status,
      Instant expiresAt
  ) {
    requireAdmin(currentUser);
    TenantEntity tenant = requireTenant(currentUser.tenantId());
    PlatformModuleEntity module = requireModule(moduleCode);
    Instant now = Instant.now();

    TenantModuleSubscriptionEntity subscription = subscriptionRepository
        .findByTenantIdAndModuleCode(tenant.getId(), moduleCode)
        .orElseGet(() -> new TenantModuleSubscriptionEntity(
            UUID.randomUUID(),
            tenant,
            module,
            TenantModuleSubscriptionStatus.DISABLED,
            null,
            now,
            null,
            now
        ));
    subscription.update(status, expiresAt, now);
    TenantModuleSubscriptionEntity saved = subscriptionRepository.save(subscription);

    auditEventService.record(
        tenant,
        actor(currentUser),
        "TENANT_MODULE_SUBSCRIPTION",
        saved.getId(),
        AuditAction.MODULE_SUBSCRIPTION_CHANGED,
        Map.of(
            "moduleCode", moduleCode.name(),
            "status", status.name()
        )
    );

    return TenantModuleSubscriptionResponse.from(saved, now);
  }

  @Transactional
  public void enableDefaultFpoModules(TenantEntity tenant) {
    Instant now = Instant.now();
    Map<ModuleCode, PlatformModuleEntity> modulesByCode = platformModuleRepository
        .findAllByOrderByCodeAsc()
        .stream()
        .collect(Collectors.toMap(PlatformModuleEntity::getCode, module -> module));

    for (ModuleCode moduleCode : DEFAULT_FPO_MODULES) {
      if (subscriptionRepository.findByTenantIdAndModuleCode(tenant.getId(), moduleCode).isPresent()) {
        continue;
      }

      PlatformModuleEntity module = modulesByCode.get(moduleCode);
      if (module == null) {
        continue;
      }

      subscriptionRepository.save(new TenantModuleSubscriptionEntity(
          UUID.randomUUID(),
          tenant,
          module,
          TenantModuleSubscriptionStatus.ENABLED,
          now,
          null,
          null,
          now
      ));
    }
  }

  private void requireAdmin(CurrentUser currentUser) {
    if (!currentUser.hasAnyRole(Role.ADMIN)) {
      throw new ApplicationException(
          ErrorCode.ACCESS_DENIED,
          "Only admins can manage module subscriptions.",
          HttpStatus.FORBIDDEN
      );
    }
  }

  private PlatformModuleEntity requireModule(ModuleCode moduleCode) {
    return platformModuleRepository.findByCode(moduleCode)
        .orElseThrow(() -> new ApplicationException(
            ErrorCode.RESOURCE_NOT_FOUND,
            "Platform module not found.",
            HttpStatus.NOT_FOUND
        ));
  }

  private TenantEntity requireTenant(UUID tenantId) {
    return tenantRepository.findById(tenantId)
        .orElseThrow(() -> new ApplicationException(
            ErrorCode.RESOURCE_NOT_FOUND,
            "Tenant not found.",
            HttpStatus.NOT_FOUND
        ));
  }

  private UserEntity actor(CurrentUser currentUser) {
    return userRepository.findById(currentUser.userId()).orElse(null);
  }
}
