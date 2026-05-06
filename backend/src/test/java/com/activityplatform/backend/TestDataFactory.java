package com.activityplatform.backend;

import com.activityplatform.backend.auth.domain.RoleEntity;
import com.activityplatform.backend.auth.domain.TenantEntity;
import com.activityplatform.backend.auth.domain.UserEntity;
import com.activityplatform.backend.security.Role;
import com.activityplatform.backend.workflow.domain.WorkflowDefinitionEntity;
import com.activityplatform.backend.workflow.domain.WorkflowDefinitionStatus;
import com.activityplatform.backend.workflow.domain.WorkflowTaskEntity;
import java.time.Instant;
import java.util.UUID;

public final class TestDataFactory {
  private TestDataFactory() {
  }

  public static TenantEntity tenant(String code) {
    Instant now = Instant.now();
    return new TenantEntity(UUID.randomUUID(), code, "Test Tenant", "ACTIVE", now);
  }

  public static RoleEntity role(TenantEntity tenant, Role role) {
    return new RoleEntity(UUID.randomUUID(), tenant, role.name(), role.name(), Instant.now());
  }

  public static UserEntity user(
      TenantEntity tenant,
      String username,
      String passwordHash,
      String displayName,
      RoleEntity... roles
  ) {
    UserEntity user = new UserEntity(
        UUID.randomUUID(),
        tenant,
        username,
        passwordHash,
        displayName,
        "+91 00000 00000",
        "Test Location",
        "Test Site",
        "ACTIVE",
        Instant.now()
    );
    for (RoleEntity role : roles) {
      user.addRole(role);
    }
    return user;
  }

  public static WorkflowDefinitionEntity workflow(
      TenantEntity tenant,
      String code,
      WorkflowDefinitionStatus status
  ) {
    WorkflowDefinitionEntity workflow = new WorkflowDefinitionEntity(
        UUID.randomUUID(),
        tenant,
        code,
        "Test Workflow",
        "agriculture",
        30,
        1,
        status,
        Instant.now()
    );
    workflow.addTask(new WorkflowTaskEntity(
        UUID.randomUUID(),
        "prepare",
        "Prepare field",
        10,
        0,
        true,
        Instant.now()
    ));
    return workflow;
  }
}
