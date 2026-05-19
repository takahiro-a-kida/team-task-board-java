package com.example.taskboard.task.dto;

import com.example.taskboard.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull TaskStatus status) {
}
