package com.example.taskboard.task.dto;

import java.time.LocalDate;
import java.util.List;
import com.example.taskboard.task.TaskPriority;
import com.example.taskboard.task.TaskStatus;

public record SearchCriteria(
        String keyword,
        List<TaskStatus> statuses,
        List<TaskPriority> priorities,
        Long assigneeUserId,
        Boolean assigneeUnassigned,
        LocalDate dueFrom,
        LocalDate dueTo
) {
    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }
}
