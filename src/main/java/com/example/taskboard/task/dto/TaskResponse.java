package com.example.taskboard.task.dto;

import java.time.Instant;
import java.time.LocalDate;
import com.example.taskboard.task.Task;
import com.example.taskboard.task.TaskPriority;
import com.example.taskboard.task.TaskStatus;
import com.example.taskboard.user.UserSummary;

public record TaskResponse(
        long id,
        long teamId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        UserSummary assignee,
        LocalDate dueDate,
        UserSummary createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse of(Task t, UserSummary assignee, UserSummary createdBy) {
        return new TaskResponse(t.id(), t.teamId(), t.title(), t.description(),
                t.status(), t.priority(), assignee, t.dueDate(), createdBy,
                t.createdAt(), t.updatedAt());
    }
}
