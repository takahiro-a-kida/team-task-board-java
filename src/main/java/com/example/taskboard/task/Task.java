package com.example.taskboard.task;

import java.time.Instant;
import java.time.LocalDate;

public record Task(
        long id,
        long teamId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        Long assigneeUserId,
        LocalDate dueDate,
        long createdByUserId,
        Instant createdAt,
        Instant updatedAt
) {
}
