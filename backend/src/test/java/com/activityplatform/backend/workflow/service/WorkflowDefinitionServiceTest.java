package com.activityplatform.backend.workflow.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.activityplatform.backend.activity.repository.ActivityRepository;
import com.activityplatform.backend.auth.repository.TenantRepository;
import com.activityplatform.backend.auth.repository.UserRepository;
import com.activityplatform.backend.audit.service.AuditEventService;
import com.activityplatform.backend.common.error.ApplicationException;
import com.activityplatform.backend.security.CurrentUser;
import com.activityplatform.backend.security.Role;
import com.activityplatform.backend.workflow.api.WorkflowRequest;
import com.activityplatform.backend.workflow.api.WorkflowTaskRequest;
import com.activityplatform.backend.workflow.domain.WorkflowDefinitionStatus;
import com.activityplatform.backend.workflow.repository.WorkflowDefinitionRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WorkflowDefinitionServiceTest {
  private final WorkflowDefinitionService service = new WorkflowDefinitionService(
      Mockito.mock(ActivityRepository.class),
      Mockito.mock(AuditEventService.class),
      Mockito.mock(TenantRepository.class),
      Mockito.mock(UserRepository.class),
      Mockito.mock(WorkflowDefinitionRepository.class)
  );

  @Test
  void rejectsDuplicateTaskCodesWithinWorkflow() {
    CurrentUser currentUser = new CurrentUser(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "admin",
        Set.of(Role.ADMIN)
    );
    WorkflowRequest request = new WorkflowRequest(
        "client-crop",
        "Client crop",
        "agriculture",
        90,
        1,
        WorkflowDefinitionStatus.ACTIVE,
        List.of(
            new WorkflowTaskRequest("task-one", "First task", 10, 0, true),
            new WorkflowTaskRequest("task-one", "Duplicate task", 20, 7, true)
        )
    );

    assertThatThrownBy(() -> service.create(currentUser, request))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("Task codes must be unique");
  }
}
