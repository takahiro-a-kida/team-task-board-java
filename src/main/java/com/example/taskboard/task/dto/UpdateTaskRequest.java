package com.example.taskboard.task.dto;

import java.time.LocalDate;
import com.example.taskboard.task.TaskPriority;
import com.example.taskboard.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        @NotNull TaskStatus status,
        @NotNull TaskPriority priority,
        Long assigneeUserId,
        LocalDate dueDate
) {
}
