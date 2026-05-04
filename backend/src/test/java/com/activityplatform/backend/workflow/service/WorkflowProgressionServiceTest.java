package com.activityplatform.backend.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.activityplatform.backend.workflow.domain.TaskProgress;
import com.activityplatform.backend.workflow.domain.TaskStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkflowProgressionServiceTest {
  private final WorkflowProgressionService service = new WorkflowProgressionService();

  @Test
  void activatesFirstPendingTaskWhenNoNextTaskExists() {
    List<TaskProgress> tasks = List.of(
        new TaskProgress("prepare", 10, TaskStatus.DONE),
        new TaskProgress("submit-proof", 20, TaskStatus.PENDING),
        new TaskProgress("review", 30, TaskStatus.PENDING)
    );

    List<TaskProgress> result = service.activateNextPending(tasks);

    assertThat(result).extracting(TaskProgress::status)
        .containsExactly(TaskStatus.DONE, TaskStatus.NEXT, TaskStatus.PENDING);
  }

  @Test
  void keepsExistingNextTaskStable() {
    List<TaskProgress> tasks = List.of(
        new TaskProgress("prepare", 10, TaskStatus.NEXT),
        new TaskProgress("submit-proof", 20, TaskStatus.PENDING)
    );

    List<TaskProgress> result = service.activateNextPending(tasks);

    assertThat(result).isEqualTo(tasks);
  }

  @Test
  void calculatesProgressFromDoneAndSkippedTasks() {
    List<TaskProgress> tasks = List.of(
        new TaskProgress("prepare", 10, TaskStatus.DONE),
        new TaskProgress("optional-review", 20, TaskStatus.SKIPPED),
        new TaskProgress("final-proof", 30, TaskStatus.PENDING)
    );

    assertThat(service.calculateProgressPercent(tasks)).isEqualTo(67);
    assertThat(service.isComplete(tasks)).isFalse();
  }
}

