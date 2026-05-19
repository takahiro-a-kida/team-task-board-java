package com.example.taskboard.task.dto;

import java.time.LocalDate;
import com.example.taskboard.task.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        @NotNull TaskPriority priority,
        Long assigneeUserId,
        LocalDate dueDate
) {
}
