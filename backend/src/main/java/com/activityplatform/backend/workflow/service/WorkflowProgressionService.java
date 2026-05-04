package com.activityplatform.backend.workflow.service;

import com.activityplatform.backend.workflow.domain.TaskProgress;
import com.activityplatform.backend.workflow.domain.TaskStatus;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WorkflowProgressionService {
  public List<TaskProgress> activateNextPending(List<TaskProgress> tasks) {
    if (tasks.stream().anyMatch(task -> task.status() == TaskStatus.NEXT)) {
      return tasks;
    }

    String nextTaskCode = tasks.stream()
        .filter(task -> task.status() == TaskStatus.PENDING)
        .min(Comparator.comparingInt(TaskProgress::sequenceNumber))
        .map(TaskProgress::taskCode)
        .orElse(null);

    if (nextTaskCode == null) {
      return tasks;
    }

    return tasks.stream()
        .map(task -> nextTaskCode.equals(task.taskCode())
            ? new TaskProgress(task.taskCode(), task.sequenceNumber(), TaskStatus.NEXT)
            : task
        )
        .toList();
  }

  public int calculateProgressPercent(List<TaskProgress> tasks) {
    if (tasks.isEmpty()) {
      return 0;
    }

    long completed = tasks.stream()
        .filter(task -> task.status() == TaskStatus.DONE || task.status() == TaskStatus.SKIPPED)
        .count();

    return Math.round((completed * 100.0f) / tasks.size());
  }

  public boolean isComplete(List<TaskProgress> tasks) {
    return !tasks.isEmpty() && tasks.stream()
        .allMatch(task -> task.status() == TaskStatus.DONE || task.status() == TaskStatus.SKIPPED);
  }
}

